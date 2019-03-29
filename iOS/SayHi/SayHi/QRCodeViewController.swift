//
//  QRCodeViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 28.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation
import AudioToolbox
import PromiseKit
import CoreLocation

let DelayGreetingDuration = 1.0
let DelayMatchDuration = 0.5

var location: CLLocation?
var locationPlacemark: CLPlacemark?

class QRCodeViewController : PlainController, UIScrollViewDelegate {
    
    @IBOutlet weak var qrCodeImageView: UIImageView!
    @IBOutlet weak var previewContainer: UIView!
    @IBOutlet weak var previewView: UIView!
    @IBOutlet weak var correctionLevelStepper: UIStepper!
    @IBOutlet weak var correctionLevelLabel: UILabel!

    @IBOutlet weak var pulseView: UIView!
    @IBOutlet weak var pulseLabel: UILabel!
    @IBOutlet weak var activityIndicator: UIActivityIndicatorView!
    @IBOutlet weak var activityLabel: UILabel!
    var activityTimer: Timer?
    
    @IBAction func donePressed(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    @IBOutlet weak var helpPulseLabel: UILabel!
    @IBOutlet weak var helpScrollView: UIScrollView!
    @IBOutlet var helpScrollContentView: UIView!
    @IBOutlet weak var helpScrollPageControl: UIPageControl!
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    @IBOutlet weak var infoButton: UIBarButtonItem!
    @IBAction func infoPressed(_ sender: Any) {
        let alertController = UIAlertController(title: "QR Code Recognition".localized, message: lastInfoMsgKey.aliasLocalized, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
        alertController.addAction(okAction)
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    @IBOutlet weak var tagBarButtonItem: UIBarButtonItem!
    
    @IBAction func correctionLevelChanged(_ sender: UIStepper) {
        setCorrectionLevelLabel()
        if !generateQR() {
            correctionLevelStepper.value -= 1
        }
        setCorrectionLevelLabel()
    }

    var pulsating = false
    var stopPulsating = false
    
    var session: String = ""
    var sessions: [String:Bool] = [:]
    var messageKey: String = ""
    var matchMessageKey: String = ""
    var matchKey: String = ""
    var matchIsFirst: Bool = false
    var lastInfoMsgKey: String = ""
    
    var geocoder = CLGeocoder()
    
    var _profile : Profile!
    var profile : Profile {
        set {
            _profile = newValue
            view.layoutSubviews()
            initialize()
        }
        get { return _profile }
    }
    
    var correctionLevelIndex = 0
    let correctionLevels = CorrectionLevel.list.map({ (correctionLevel) -> [String:Any] in
        return ["key": correctionLevel, "name": correctionLevel.rawValue.codeLocalized]
    })

    override func viewDidLoad() {
        super.viewDidLoad()
        
        qrCodeImageView.layer.borderColor = UIColor(white: 1.0, alpha: 0.3).cgColor
        qrCodeImageView.layer.borderWidth = 1
        
        previewView.layer.borderColor = UIColor(white: 1.0, alpha: 0.6).cgColor
        previewView.layer.borderWidth = 1
        previewView.layer.cornerRadius = 5
        previewView.clipsToBounds = true
        
        pulseView.layer.cornerRadius = pulseView.bounds.size.width / 2.0
        pulseView.layer.borderColor = UIColor.white.cgColor
        pulseView.layer.borderWidth = 5.0
        
        helpScrollContentView.backgroundColor = .clear
        helpScrollView.addSubview(helpScrollContentView)
        helpScrollView.contentSize = helpScrollContentView.bounds.size
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(didScrollView))
        tapGesture.numberOfTapsRequired = 1
        helpScrollView.addGestureRecognizer(tapGesture)
        
        NotificationCenter.default.addObserver(self, selector: #selector(updatedLocation), name: UpdateLocationNotification, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc func didScrollView() {
        helpView.hide()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.startReadQR()
        clearInfo()
        UIApplication.shared.isIdleTimerDisabled = true
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.stopReadQR()
        if self.navigationController!.isBeingDismissed || self.navigationController!.isMovingFromParent {
            self.destroySession()
        }
        UIApplication.shared.isIdleTimerDisabled = false
    }
    
    func initialize() {
        setCorrectionLevelLabel()
        initSession()
    }

    func shouldShowQRHelpFirst() -> Bool {
        return !UserData.instance.qrHelpFirstShown && !Settings.instance.disableHelpQR
    }
    
    func showQRHelpFirst() -> Bool {
        if shouldShowQRHelpFirst() {
            UserData.instance.qrHelpFirstShown = true
            UserData.instance.touch()
            let dispatchTime = DispatchTime.now() + 0.5
            DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
                self.helpView.show(owner: self)
            }
            return true
        }
        return false
    }
    
    func hideBarButtonItems(_ buttons : [String]) {
        for button in buttons {
            switch button {
            case "tag":
                if let index = self.navigationItem.rightBarButtonItems?.firstIndex(of: tagBarButtonItem) {
                    self.navigationItem.rightBarButtonItems?.remove(at: index)
                }
            default:
                break
            }
        }
    }
    
    func startLocation() {
        AppDelegate.instance.startLocation()
    }
    
    func stopLocation() {
        AppDelegate.instance.stopLocation()
    }
    
    @objc func updatedLocation(notification: NSNotification) {
        location = notification.object as? CLLocation
        if let location = location {
            geocoder.reverseGeocodeLocation(location) { (placemarks, error) in
                if let placemark = placemarks?.first {
                    locationPlacemark = placemark
                }
            }
        }
    }
    
    func startPulse() {
        if !pulsating {
            self.pulseShrink()
        }
        pulsating = true
    }
    
    func pulseShrink() {
        if stopPulsating {
            return
        }
        UIView.animate(withDuration: 1.0, animations: {
            self.pulseView.transform = CGAffineTransform(scaleX: 0.75, y: 0.75)
        }, completion: { (completed) in
            self.pulseGrow()
        })
    }
    
    func pulseGrow() {
        if stopPulsating {
            return
        }
        UIView.animate(withDuration: 1.0, animations: {
            self.pulseView.transform = CGAffineTransform.identity
        }, completion: { (completed) in
            self.pulseShrink()
        })
    }
    
    func setCorrectionLevelLabel() {
        correctionLevelIndex = Int(correctionLevelStepper.value)
        correctionLevelLabel.text = correctionLevels[correctionLevelIndex]["name"] as? String
    }
    
    func startReadQR() {
        stopPulsating = false
        if Platform.isDevice {
            QRCode.instance.startRead(owner: self, previewView: previewView) { started in
                if started {
                    self.helpPulseLabel.text = "QRCodeRecognitionActive".aliasLocalized
                    DispatchQueue.main.async {
                        self.startPulse()
                        _ = self.showQRHelpFirst()
                        self.startLocation()
                    }
                } else {
                    self.helpPulseLabel.text = "QRCodeRecognitionNotActive".aliasLocalized
                    self.pulseLabel.isHidden = false
                    UIView.animate(withDuration: 0.5, animations: {
                        self.pulseLabel.alpha = 1.0
                    })
                }
            }
        } else {
            self.helpPulseLabel.text = "QRCodeRecognitionNotActive".aliasLocalized
            self.pulseLabel.isHidden = false
            UIView.animate(withDuration: 0.5, animations: {
                self.pulseLabel.alpha = 1.0
            })
        }
        NotificationCenter.default.addObserver(self, selector: #selector(qrCodeRecognized), name: QRCodeRecognizedNotification, object: nil)
    }
    
    func stopReadQR() {
        pulsating = false
        stopPulsating = true
        if Platform.isDevice {
            QRCode.instance.stopRead()
            stopLocation()
        }
        NotificationCenter.default.removeObserver(self)
    }

    @objc func checkActivity() {
        UIView.animate(withDuration: 0.5, animations: {
            self.activityLabel.alpha = 1.0
        })
    }
    
    func initSession() {
        self.session = Crypto.toBase64(Crypto.generateRandom(length: MatchSessionLength)).prefixStr(MatchSessionLength)
        let messageContent = generateMessageContent(passcode: self.session)
        _ = DataService.instance.createMessage(messageContent).then { (messageKey) -> Void in
            self.messageKey = messageKey
            _ = self.generateQR()
            self.correctionLevelStepper.isEnabled = true
            self.activityTimer?.invalidate()
            UIView.animate(withDuration: 0.1, animations: {
                self.activityIndicator.alpha = 0.0
                self.activityLabel.alpha = 0.0
            }, completion: { (completed) in
                self.activityIndicator.isHidden = true
                self.activityIndicator.stopAnimating()
                self.activityLabel.isHidden = true
            })
            if !UserData.instance.matchHandshake {
                _ = DataService.instance.observeMessageMatch(messageKey) { (matchMessageKey) in
                    self.observeMatchStatus(matchMessageKey)
                }.catch { (error) in
                    self.info("UnexpectedErrorOccurred")
                }
            }
        }.catch { (error) in
            let alertController = UIAlertController(title: "Network Error".localized, message: "UnexpectedErrorOccurred".aliasLocalized, preferredStyle: .alert)
            let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
            alertController.addAction(okAction)
            alertController.view?.tintColor = AccentColor
            self.present(alertController, animated: true, completion: nil)
        }
        activityTimer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(checkActivity), userInfo: nil, repeats: false)
    }
    
    func destroySession() {
        removeMessage()
        removeMatch()
        self.session = ""
    }
    
    func removeMessage() {
        DataService.instance.stopObserveMessageMatch()
        if !self.messageKey.isEmpty {
            _ = DataService.instance.removeMessage(messageKey)
            self.messageKey = ""
        }
    }
    
    func removeMatch() {
        DataService.instance.stopObserveMatchStatus()
        if !self.matchKey.isEmpty {
            let matchKey = self.matchKey
            _ = DataService.instance.setMatchPartInactive(matchKey, isFirst: matchIsFirst).always {
                _ = DataService.instance.removeInactiveMatch(matchKey)
            }
            self.matchKey = ""
            self.matchIsFirst = false
        }
    }
    
    func generateMessageContent(passcode: String) -> String {
        return AESCrypt(passcode: passcode).encrypt(Message.generate(profile: profile).description)
    }
    
    func parseMessageContent(content: String, passcode: String) -> Message? {
        return Message.parse(AESCrypt(passcode: passcode).decrypt(content))
    }
    
    func readMessageContent(_ messageKey: String) -> Promise<String?> {
        return DataService.instance.messageContent(messageKey)
    }
    
    func generateQRImage() -> UIImage? {
        let content = QRContent.generate(message: messageKey, session: session).description
        let encryptedContent = AESCrypt(passcode: AppSharedAPIKey).encrypt(content)
        let correctionLevel = correctionLevels[correctionLevelIndex]["key"] as! CorrectionLevel
        return QRCode.instance.generate(text: encryptedContent, size: qrCodeImageView.bounds.width, correctionLevel: correctionLevel)
    }
    
    func parseQRImage(content: String) -> QRContent? {
        let decryptedContent = AESCrypt(passcode: AppSharedAPIKey).decrypt(content)
        return QRContent.parse(decryptedContent)
    }
    
    func generateQR() -> Bool {
        let newImage = generateQRImage()
        if newImage != nil {
            qrCodeImageView.image = nil
            qrCodeImageView.image = newImage
        } else {
            let alertController = UIAlertController(title: "QR Code Generation".localized, message: "QRCodeImageNotAvailable".aliasLocalized, preferredStyle: .alert)
            alertController.view?.tintColor = AccentColor
            self.present(alertController, animated: true, completion: nil)
            let dispatchTime = DispatchTime.now() + 2.0
            DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
                alertController.dismiss(animated: true, completion: nil)
            }
        }
        return newImage != nil
    }
    
    @objc func qrCodeRecognized(notification: NSNotification) {
        var infoMsgKey = "InvalidQR"
        let content = notification.object as! String
        if !content.isEmpty {
            if let qrContent = parseQRImage(content: content) {
                if !qrContent.message.isEmpty && !qrContent.session.isEmpty && qrContent.session != self.session {
                    observeMatchStatus(qrContent.message, matchMessageSession: qrContent.session)
                    return
                } else {
                    infoMsgKey = "MatchModeNotOpen"
                }
            }
        }
        info(infoMsgKey)
        self.matchMessageKey = ""
    }

    func observeMatchStatus(_ matchMessageKey: String, matchMessageSession: String? = nil) {
        if (self.matchMessageKey != matchMessageKey) {
            self.matchMessageKey = matchMessageKey
            _ = readMessageContent(matchMessageKey).then { (messageContent) -> Void in
                if let messageContent = messageContent {
                    if !UserData.instance.matchHandshake {
                        _ = DataService.instance.updateMessageMatch(matchMessageKey, match: self.messageKey).catch { (error) in
                            self.info("UnexpectedErrorOccurred")
                        }
                    }
                    self.removeMatch()
                    self.matchKey = self.calcMatchKey(matchMessageKey)
                    self.matchIsFirst = self.isMatchFirst(matchMessageKey)
                    _ = DataService.instance.createMatchPart(self.matchKey, isFirst: self.matchIsFirst, session: !UserData.instance.matchHandshake ? self.session : "").then { () -> (Promise<Void>) in
                        self.info("MatchIsProcessed", suppressVibrate: true)
                        return DataService.instance.observeMatchStatus(self.matchKey) { (matchStatus) in
                            if matchStatus.active1 != nil && matchStatus.active2 != nil {
                                var passcode = matchMessageSession
                                if passcode == nil {
                                    if self.matchIsFirst {
                                        passcode = matchStatus.session2
                                    } else {
                                        passcode = matchStatus.session1
                                    }
                                }
                                if passcode != nil && !passcode!.isEmpty {
                                    if let message = self.parseMessageContent(content: messageContent, passcode: passcode!) {
                                        if message.space == SecureStore.spaceRefName {
                                            if self.sessions[passcode!] == nil {
                                                self.sessions[passcode!] = true
                                                self.matched(message: message, matchMessageSession: passcode!, matchIsFirst: self.matchIsFirst)
                                            } else {
                                                self.info("AlreadyMatched")
                                            }
                                        } else {
                                            self.info("DifferentSpace")
                                        }
                                    } else {
                                        self.info("InvalidQR")
                                    }
                                } else {
                                    self.info("WaitingForOtherSide", suppressVibrate: true)
                                }
                            } else {
                                self.info("WaitingForOtherSide", suppressVibrate: true)
                            }
                        }.catch { (error) in
                            self.info("UnexpectedErrorOccurred")
                        }
                    }.catch { (error) in
                        self.info("UnexpectedErrorOccurred")
                    }
                } else {
                    self.info("DifferentSpace")
                }
            }.catch { (error) in
                self.info("UnexpectedErrorOccurred")
            }
        }
    }
    
    func isMatchFirst(_ matchMessageKey: String) -> Bool {
        return messageKey < matchMessageKey
    }
    
    func calcMatchKey(_ matchMessageKey: String) -> String {
        if isMatchFirst(matchMessageKey) {
            return "\(messageKey)\(matchMessageKey)"
        } else {
            return "\(matchMessageKey)\(messageKey)"
        }
    }
    
    func matched(message: Message, matchMessageSession: String, matchIsFirst: Bool) {
        self.matchMessageKey = ""
        self.clearInfo()
        self.stopReadQR()
        DataService.instance.stopObserveMatchStatus()
        let match = Match.calculateMatch(profile: self.profile, message: message)
        if let location = location {
            match.locationLatitude = "\(location.coordinate.latitude)"
            match.locationLongitude = "\(location.coordinate.longitude)"
        }
        if let placemark = locationPlacemark {
            match.locationName = placemark.name ?? ""
            match.locationStreet = placemark.thoroughfare ?? ""
            match.locationCity = placemark.locality ?? ""
            match.locationCountry = placemark.country ?? ""
        }
        UserData.instance.addMatch(match)
        NotificationCenter.default.post(name: UserDataMatchNotification, object: nil)
        Analytics.instance.logMatch(match: match, session: session, messageSession: matchMessageSession)
        if matchIsFirst {
            self.notifyMatch()
        } else {
            let dispatchTime = DispatchTime.now() + DelayGreetingDuration
            DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
                self.notifyMatch()
            }
        }
        let dispatchTime = DispatchTime.now() + DelayMatchDuration
        DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
            self.performSegue(withIdentifier: "match", sender: match)
        }
    }
    
    func notifyMatch() {
        clearInfo()
        if UserData.instance.matchVibrate && Platform.isDevice {
            if #available(iOS 10.0, *) {
                let generator = UINotificationFeedbackGenerator()
                generator.prepare()
                if "\(generator)".contains("prepared=1") {
                    generator.notificationOccurred(.success)
                } else {
                    AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
                }
            } else {
                AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        }
        
        if UserData.instance.matchPlayGreeting {
            let playSynth = {
                Voice.instance.sayHi(presenter: self, name: UserData.instance.firstName, gender: UserData.instance.gender)
            }
            if let voiceData = UserData.instance.greetingVoice {
                if !Voice.instance.replay(presenter: self, voice: voiceData) {
                    playSynth()
                }
            } else {
                playSynth()
            }
        }
    }
    
    func info(_ key: String, suppressVibrate: Bool = false) {
        infoButton.isEnabled = true
        if lastInfoMsgKey != key {
            lastInfoMsgKey = key
            if !suppressVibrate && !lastInfoMsgKey.isEmpty {
                if UserData.instance.matchVibrate && Platform.isDevice {
                    if #available(iOS 10.0, *) {
                        let generator = UINotificationFeedbackGenerator()
                        generator.notificationOccurred(.warning)
                    }
                }
            }
        }
    }
    
    func clearInfo() {
        infoButton.isEnabled = false
        lastInfoMsgKey = ""
    }
    
    @IBAction func pageChanged(_ sender: Any) {
        updateScrollOffset()
    }
    
    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate {
            updatePaging()
        }
    }
    
    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        updatePaging()
    }
    
    func updateScrollOffset() {
        let x = CGFloat(Float(helpScrollPageControl.currentPage) * Float(helpScrollView.frame.size.width))
        helpScrollView.setContentOffset(CGPoint(x: x, y: 0), animated: true)
    }
    
    func updatePaging() {
        let pageNumber = Int(roundf(Float(helpScrollView.contentOffset.x) / Float(helpScrollView.frame.size.width)))
        helpScrollPageControl.currentPage = pageNumber
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "match",
            let matchViewController = segue.destination as? MatchViewController {
            matchViewController.match = sender as! Match
            matchViewController.profile = profile
        } else if segue.identifier == "history",
            let historyTableController = segue.destination as? HistoryTableController {
            historyTableController.profile = profile
        } else if segue.identifier == "profile",
            let tagViewController = segue.destination as? TagViewController {
            tagViewController.profile = profile
            tagViewController.makeReadOnly()
        }
    }
}
