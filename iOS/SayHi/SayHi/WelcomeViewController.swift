//
//  WelcomeViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 22.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

let TransformRatio : CGFloat = 0.28
let StayDelay = 0.5
let MoveTransition = 0.75
let FadeTransition = 0.5
let QRTransparent: CGFloat = 0.1
let IconSize: CGFloat = 70

class WelcomeViewController: PlainController, ButtonImageDelegate, ButtonLabelDelegate, UITextFieldDelegate, UIPickerViewDelegate, UIPickerViewDataSource, UIScrollViewDelegate,
SettingsLanguageTableControllerDelegate {

    let ProtectedSegues = ["sayhi", "settings", "profiles", "history", "currentProfile", "highscore"]
    
    let year : Int = Calendar.current.dateComponents([.year], from: Date()).year!
    
    @IBOutlet weak var logoLabel: UILabel!
    @IBOutlet weak var nameButton: UIButton!
    @IBOutlet weak var statusTextField: UITextField!

    @IBOutlet weak var profilesButton: UIBarButtonItem!
    
    @IBOutlet weak var currentQRPreview: ButtonImage!
    @IBOutlet weak var currentProfileLabel: ButtonLabel!
    @IBOutlet weak var matchingHistoryLabel: ButtonLabel!
    @IBOutlet weak var currentProfileHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var currentProfileBottomConstraint: NSLayoutConstraint!
    
    @IBOutlet weak var setupContainerView: UIView!
    @IBOutlet weak var setupWelcomeLabel: UILabel!
    @IBOutlet weak var setupScrollView: UIScrollView!
    @IBOutlet weak var setupContentView: UIView!
    @IBOutlet weak var setupContainerBottomConstraint: NSLayoutConstraint!
    @IBOutlet weak var setupPageControl: UIPageControl!
    @IBOutlet weak var setupCancelButton: UIButton!
    @IBOutlet weak var setupNextButton: UIButton!
    @IBOutlet weak var setupFirstNameTextField: UITextField!
    @IBOutlet weak var setupLanguageSegmentedControl: UISegmentedControl!
    @IBOutlet weak var setupGenderSegmentedControl: UISegmentedControl!
    @IBOutlet weak var setupBirthYearPickerView: UIPickerView!
    @IBOutlet weak var setupOtherLanguageButton: UIButton!
    @IBOutlet weak var setupFirstNameTestButton: UIButton!
    
    @IBOutlet weak var setupGenderTestButton: UIButton!
    @IBOutlet weak var nameTopConstraint: NSLayoutConstraint!
    @IBOutlet weak var statusTopConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpStartMatchTopConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpStatusTopConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpActiveProfileBottomConstraint: NSLayoutConstraint!
    
    @IBOutlet weak var helpMatchingScoreLabel: UILabel!
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }

    var scoreLabel: ButtonLabel!
    var okAlertAction : UIAlertAction?
    
    var leftBarButtonItems : [UIBarButtonItem] = []
    var rightBarButtonItems : [UIBarButtonItem] = []
    
    var iconEffectView: UIVisualEffectView!
    var iconLabel: UILabel!
    var iconImage: UIImageView!
    
    var introCompleted: Bool = false
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        currentQRPreview.delegate = self
        currentProfileLabel.delegate = self
        matchingHistoryLabel.delegate = self
        
        scoreLabel = ButtonLabel(frame: CGRect(x:0, y:0, width:120, height:50))
        scoreLabel.backgroundColor = UIColor.clear
        scoreLabel.numberOfLines = 2
        scoreLabel.font = UIFont.boldSystemFont(ofSize: 16.0)
        scoreLabel.textAlignment = .center
        scoreLabel.textColor = UIColor.white
        scoreLabel.attributedText = nil
        scoreLabel.isHidden = true
        scoreLabel.delegate = self
        self.navigationItem.titleView = scoreLabel
        updateScore()
        
        updateCurrentProfileContent()
        
        NotificationCenter.default.addObserver(self, selector: #selector(showContentEvent), name: ShowContentNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleUserDataEvent), name: UserDataFetchedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleUserDataEvent), name: UserDataChangedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleUserDataEvent), name: UserDataClearedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleUserDataEvent), name: UserDataMatchNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(spaceSwitched), name: SpaceSwitchedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(settingsFetched), name: SettingsFetchedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(contentLanguageChanged), name: UserDataLangChangedNotification, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow), name: NSNotification.Name.UIKeyboardWillShow, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide), name: NSNotification.Name.UIKeyboardWillHide, object: nil)
        
        let dispatchTime = DispatchTime.now() + StayDelay
        DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
            self.animateLogo()
            self.animateText()
        }
        
        self.setupScrollView.layer.cornerRadius = 15
        self.setupScrollView.clipsToBounds = true
        self.setupScrollView.addSubview(setupContentView)
        self.setupScrollView.contentSize = setupContentView.bounds.size
        
        self.currentProfileLabel.alpha = 0.0
        self.currentQRPreview.alpha = 0.0
        self.scoreLabel.alpha = 0.0
        self.statusTextField.alpha = 0.0
        self.matchingHistoryLabel.alpha = 0.0
        self.setupContainerView.alpha = 0.0
        
        addIcon()
        
        leftBarButtonItems = self.navigationItem.leftBarButtonItems!
        self.navigationItem.leftBarButtonItems = nil
        self.navigationItem.leftBarButtonItem = nil
        rightBarButtonItems = self.navigationItem.rightBarButtonItems!
        self.navigationItem.rightBarButtonItems = nil
        self.navigationItem.rightBarButtonItem = nil
        
        if !SecureStore.appInitialized() {
            nameButton.isUserInteractionEnabled = false
            setupBirthYearPickerView.selectRow(30, inComponent: 0, animated: false)
            updateContentLanguage()
        }
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(didTap))
        self.view.addGestureRecognizer(tapGesture)
        
        if isIPhone5() {
            nameTopConstraint.constant = 0.0
            statusTopConstraint.constant = 0.0
            helpStartMatchTopConstraint.constant = 110.0
            helpStatusTopConstraint.constant = 135.0
        }
        
        if isIPhoneX() {
            currentProfileBottomConstraint.constant = 25
            helpActiveProfileBottomConstraint.constant = helpActiveProfileBottomConstraint.constant + 15
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        profilesButton.isEnabled = false
        profilesButton.isEnabled = true
    }
    
    @objc override func colorsSet() {
        super.colorsSet()
        setupWelcomeLabel.textColor = AccentColor
        setupCancelButton.setTitleColor(AccentColor, for: .normal)
        setupNextButton.setTitleColor(AccentColor, for: .normal)
        setupFirstNameTestButton.setTitleColor(AccentColor, for: .normal)
        setupFirstNameTestButton.setImage(UIImage(named: "play")!.colored(AccentColor), for: .normal);
        setupGenderTestButton.setTitleColor(AccentColor, for: .normal)
        setupGenderTestButton.setImage(UIImage(named: "play")!.colored(AccentColor), for: .normal);
        setupOtherLanguageButton.setTitleColor(AccentColor, for: .normal)
        setupGenderSegmentedControl.tintColor = AccentColor
        setupLanguageSegmentedControl.tintColor = AccentColor
        setupBirthYearPickerView.reloadAllComponents()
        updateCurrentProfileContent()
    }
    
    @objc func spaceSwitched() {
        let dispatchTime = DispatchTime.now() + 1.0
        DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
            let alertController = UIAlertController(title: "Space Switch".localized, message: "SpaceSwitchedSuccessfully.".aliasLocalized, preferredStyle: .alert)
            
            let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            })
            alertController.addAction(okAction)
            
            alertController.view?.tintColor = AccentColor
            self.activeViewController.present(alertController, animated: true, completion: nil)
        }
    }
    
    @objc func settingsFetched() {
        setLogoImage()
        updateScore()
        updateCurrentProfileContent()
    }
    
    @objc func contentLanguageChanged() {
        updateContentLanguage()
    }
    
    func updateScore() {
        scoreLabel.active = !(Settings.instance.disableHighscoreShare && Settings.instance.disableHighscoreShow)
        helpMatchingScoreLabel.text = scoreLabel.active ?
            "DisplaysMatchingScoreHighscore".aliasLocalized : "DisplaysMatchingScore".aliasLocalized

        if Settings.instance.highscoreLocal.isEmpty {
            UserData.instance.localScore = 0
            UserData.instance.scoreLocalCount = 0
        } else if (UserData.instance.highscoreLocal != Settings.instance.highscoreLocal) {
            UserData.instance.localScore = UserData.instance.matchScore
            UserData.instance.scoreLocalCount = UserData.instance.scoreMatchCount
        }
        UserData.instance.highscoreLocal = Settings.instance.highscoreLocal
    }
    
    func updateContentLanguage() {
        switch UserData.instance.langCode {
        case "en":
            setupLanguageSegmentedControl.selectedSegmentIndex = 0
            setupOtherLanguageButton.setTitle("Other...".localized, for: .normal)
        case "de":
            setupLanguageSegmentedControl.selectedSegmentIndex = 1
            setupOtherLanguageButton.setTitle("Other...".localized, for: .normal)
        default:
            setupLanguageSegmentedControl.selectedSegmentIndex = UISegmentedControlNoSegment
            setupOtherLanguageButton.setTitle("\(Locale.current.localizedString(forIdentifier: UserData.instance.langCode) ?? "")", for: .normal)
        }
    }
    
    func setLogoImage() {
        if Settings.instance.logoPlain {
            if Settings.instance.logoZoom == 1.0 {
                iconImage.frame = iconEffectView.frame
            } else {
                let delta = CGFloat(Settings.instance.logoZoom - 1.0) * iconEffectView.frame.width / 2.0
                iconImage.frame = iconEffectView.frame.insetBy(dx: -delta, dy: -delta)
            }
            iconEffectView.isHidden = true
        } else {
            iconImage.frame = iconEffectView.frame.insetBy(dx: 5, dy: 5)
            iconEffectView.isHidden = false
        }
        if let logo = Settings.instance.logo {
            iconImage.image = logo
            iconImage.isHidden = false
            iconLabel.isHidden = true
        } else {
            iconImage.image = nil
            iconImage.isHidden = true
            iconLabel.isHidden = false
        }
    }
    
    func addIcon() {
        iconEffectView = UIVisualEffectView(effect: UIBlurEffect(style: .extraLight))
        iconEffectView.frame = CGRect(x: (currentQRPreview.bounds.size.width - IconSize) / 2.0,
                                      y: (currentQRPreview.bounds.size.width - IconSize) / 2.0,
                                      width: IconSize, height: IconSize)
        iconEffectView.layer.cornerRadius = IconSize / 2.0
        iconEffectView.layer.borderColor = UIColor.colorWithHexString(hexString: "#ffffff", alpha: 0.3).cgColor
        iconEffectView.layer.borderWidth = 5
        iconEffectView.clipsToBounds = true
        
        let vibrancyEffectView = UIVisualEffectView(effect: UIVibrancyEffect(blurEffect: UIBlurEffect(style: .extraLight)))
        vibrancyEffectView.frame = iconEffectView.bounds
        iconEffectView.contentView.addSubview(vibrancyEffectView)

        iconLabel = UILabel()
        iconLabel.textColor = .white
        iconLabel.text = "i!".localized
        iconLabel.font = UIFont.systemFont(ofSize: 55, weight: UIFont.Weight.light)
        iconLabel.textAlignment = .center
        iconLabel.frame = iconEffectView.bounds
        vibrancyEffectView.contentView.addSubview(iconLabel)

        currentQRPreview.addSubview(iconEffectView)
        
        iconImage = UIImageView()
        iconImage.frame = iconEffectView.frame.insetBy(dx: 5, dy: 5)
        iconImage.backgroundColor = UIColor.clear
        iconImage.contentMode = .scaleAspectFit
        iconImage.isHidden = true
        
        currentQRPreview.addSubview(iconImage)
        
        setLogoImage()
    }
    
    func animateLogo() {
        UIView.animate(withDuration: MoveTransition, delay: 0, options: .curveEaseInOut, animations: {
            self.logoLabel.center = CGPoint(x: self.nameButton.center.x + self.nameButton.frame.size.width / 2.0 - self.logoLabel.frame.width / 2.0 - 0.5,
                                            y: self.nameButton.center.y)
            self.logoLabel.transform = CGAffineTransform(scaleX: TransformRatio, y: TransformRatio)
        })
    }
    
    func animateText() {
        self.nameButton.alpha = 0.0
        UIView.animate(withDuration: FadeTransition, delay: FadeTransition, options: .curveLinear, animations: {
            self.nameButton.alpha = 1.0
        }, completion: { (completed) in
            self.logoLabel.alpha = 0.0
            self.introCompleted = true
            self.showContent()
        })
    }
    
    @objc func showContentEvent(_ sender: Any) {
        if introCompleted {
            showContent()
        }
    }
    
    func showContent() {
        let showActiveContent = { (completion: ((Bool) -> Swift.Void)?) in
            self.updateCurrentProfileContent()
            UIView.animate(withDuration: FadeTransition, delay: StayDelay, options: .curveLinear, animations: {
                self.currentQRPreview.isHidden = false
                self.currentQRPreview.alpha = 1.0
                self.scoreLabel.isHidden = false
                self.scoreLabel.alpha = 1.0
                self.statusTextField.isHidden = false
                self.statusTextField.alpha = 1.0
                self.matchingHistoryLabel.isHidden = false
                self.matchingHistoryLabel.alpha = 1.0
                self.currentProfileLabel.isHidden = false
                self.currentProfileLabel.alpha = 1.0
            }, completion: completion)
        }
        let showInactiveContent = { (completion: ((Bool) -> Swift.Void)?) in
            UIView.animate(withDuration: FadeTransition, delay: StayDelay, options: .curveLinear, animations: {
                self.currentQRPreview.isHidden = false
                self.currentQRPreview.alpha = QRTransparent
                self.scoreLabel.isHidden = false
                self.scoreLabel.alpha = 0.0
                self.statusTextField.isHidden = false
                self.statusTextField.alpha = 0.0
                self.matchingHistoryLabel.isHidden = false
                self.matchingHistoryLabel.alpha = 1.0
                self.currentProfileLabel.isHidden = false
                self.currentProfileLabel.alpha = 1.0
            }, completion: completion)
        }
        let showSetupContent = {
            UIView.animate(withDuration: FadeTransition, delay: StayDelay, options: .curveLinear, animations: {
                self.setupContainerView.isHidden = false
                self.setupContainerView.alpha = 1.0
            }, completion: { (completed) in
                let dispatchTime = DispatchTime.now() + 0.25
                DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
                    if self.setupPageControl.currentPage == 0 {
                        self.setupFirstNameTextField.becomeFirstResponder()
                    }
                }
            })
        }
        if SecureStore.appInitialized() {
            UIView.setAnimationsEnabled(false)
            self.navigationItem.leftBarButtonItems = self.leftBarButtonItems
            self.navigationItem.rightBarButtonItems = self.rightBarButtonItems
            UIView.setAnimationsEnabled(true)
            if UserData.instance.initialized {
                showActiveContent(nil)
            } else if !SecureStore.touchIDUsed {
                UserData.instance.initialize {
                    showActiveContent(nil)
                }
            } else if UserData.instance.requestInitialize {
                showInactiveContent { (completed) in
                    UserData.instance.initialize {
                        showActiveContent(nil)
                    }
                }
            } else {
                showInactiveContent(nil)
            }
        } else {
            showSetupContent()
        }
    }
    
    @objc func handleUserDataEvent(_ sender: Any) {
        updateCurrentProfileContent()
    }
    
    func updateCurrentProfileContent() {
        if SecureStore.appInitialized(),
            let profile = UserData.instance.currentProfile {
            let scoreText = NSMutableAttributedString(
                string: String(format: "%i", UserData.instance.shareScore),
                attributes: [NSAttributedStringKey.foregroundColor: AccentColor,
                             NSAttributedStringKey.font: UIFont.systemFont(ofSize: 17.0, weight: UIFont.Weight.semibold)])
            if UserData.instance.shareScore == 1 {
                scoreText.append(NSMutableAttributedString(
                    string: "\n" +
                        "Matching Point".localized,
                    attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                                 NSAttributedStringKey.font: UIFont.systemFont(ofSize: 13.0)]))
            } else {
                scoreText.append(NSMutableAttributedString(
                    string: "\n" +
                        "Matching Points".localized,
                    attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                                 NSAttributedStringKey.font: UIFont.systemFont(ofSize: 13.0)]))
            }
            self.scoreLabel.attributedText = scoreText

            var height : CGFloat = 60
            let currentProfileText = NSMutableAttributedString(
                string: "Current Profile".localized + "\n",
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.white])
            currentProfileText.append(NSMutableAttributedString(
                string: profile.name,
                attributes: [NSAttributedStringKey.foregroundColor: AccentColor]))
            var separator = "\n"
            if profile.relationType != .none {
                currentProfileText.append(NSMutableAttributedString(
                    string: separator + Emoji.relationType + profile.relationType.rawValue.codeLocalized,
                    attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                                 NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
                separator = SeparatorString
            }
            if let matchMode = profile.matchMode {
                currentProfileText.append(NSMutableAttributedString(
                    string: separator + Emoji.matchMode + matchMode.description.codeLocalized,
                    attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                                 NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))

            }
            if profile.relationType != .none || profile.matchMode != nil {
                height += 30
            }
            self.currentProfileHeightConstraint.constant = height
            UIView.animate(withDuration: 0.2, animations: { 
                self.view.layoutIfNeeded()
            })
            self.currentProfileLabel.attributedText = currentProfileText

            let qrContent = "http://sayhi-app.com?_=" + Crypto.hash(Message.generate(profile: profile).description).prefix(20)
            self.currentQRPreview.image = nil
            self.currentQRPreview.image = QRCode.instance.generate(text: qrContent, size: self.currentQRPreview.bounds.width, correctionLevel: CorrectionLevel.quartile)
            
            setStatusText()
            
            self.matchingHistoryLabel.text = String(format: "Matching History (%i)".localized, UserData.instance.scoreMatchCount)
            
            UIView.animate(withDuration: 2 * FadeTransition, animations: {
                self.currentQRPreview.alpha = 1.0
                self.scoreLabel.alpha = 1.0
                self.statusTextField.alpha = 1.0
                self.matchingHistoryLabel.alpha = 1.0
            })
        } else {
            self.scoreLabel.text = ""
            
            self.currentProfileHeightConstraint.constant = 60
            UIView.animate(withDuration: 0.2, animations: {
                self.view.layoutIfNeeded()
            })
            
            if SecureStore.appInitialized() && !UserData.instance.initialized {
                let text = NSMutableAttributedString(
                    string: "Profile Locked".localized + "\n",
                    attributes: [NSAttributedStringKey.foregroundColor: UIColor.white])
                text.append(NSMutableAttributedString(
                    string: "[Press to Unlock]".localized,
                    attributes: [NSAttributedStringKey.foregroundColor: AccentColor]))
                self.currentProfileLabel.attributedText = text
            } else {
                self.currentProfileLabel.text = ""
            }
            
            self.currentQRPreview.image = nil
            self.currentQRPreview.image = QRCode.instance.generate(text: AppName, size: self.currentQRPreview.bounds.width, correctionLevel: CorrectionLevel.low)
            
            setStatusText(true)

            self.matchingHistoryLabel.text = "Matching History".localized
            
            UIView.animate(withDuration: 2 * FadeTransition, animations: {
                self.currentQRPreview.alpha = QRTransparent
                self.scoreLabel.alpha = 0.0
                self.statusTextField.alpha = 0.0
                self.matchingHistoryLabel.alpha = 1.0
            })
        }
    }
    
    func setStatusText(_ forceEmpty: Bool = false) {
        if !UserData.instance.status.isEmpty && !forceEmpty {
            self.statusTextField.text = String(format: "»%@«".localized, UserData.instance.status)
        } else {
            self.statusTextField.text = "» «".localized
        }
    }
    
    // MARK: Keyboard Notifications
    @objc func keyboardWillShow(sender: NSNotification) {
        let info = sender.userInfo!
        let keyboardSize = (info[UIKeyboardFrameEndUserInfoKey] as! NSValue).cgRectValue.height
        setupContainerBottomConstraint.constant = keyboardSize - bottomLayoutGuide.length
        if !UserData.instance.initialized {
            if isIPhone5() {
                nameTopConstraint.constant = -30.0
            } else {
                nameTopConstraint.constant = -10.0
            }
        }
        let duration: TimeInterval = (info[UIKeyboardAnimationDurationUserInfoKey] as! NSNumber).doubleValue
        UIView.animate(withDuration: duration) {
            self.view.layoutIfNeeded()
        }
    }
    
    @objc func keyboardWillHide(sender: NSNotification) {
        let info = sender.userInfo!
        setupContainerBottomConstraint.constant = 64
        if !UserData.instance.initialized {
            if isIPhone5() {
                nameTopConstraint.constant = 0.0
            } else {
                nameTopConstraint.constant = 30.0
            }
        }
        let duration: TimeInterval = (info[UIKeyboardAnimationDurationUserInfoKey] as! NSNumber).doubleValue
        UIView.animate(withDuration: duration) {
            self.view.layoutIfNeeded()
        }
    }
    
    func didPressButtonImage(_ sender: ButtonImage) {
        if sender == currentQRPreview {
            if shouldPerformSegue(withIdentifier: "sayhi", sender: currentQRPreview) {
                self.performSegue(withIdentifier: "sayhi", sender: currentQRPreview)
            }
        }
    }
    
    func didPressButtonLabel(_ sender: ButtonLabel) {
        if sender == currentProfileLabel {
            if shouldPerformSegue(withIdentifier: "currentProfile", sender: currentProfileLabel) {
                self.performSegue(withIdentifier: "currentProfile", sender: currentProfileLabel)
            }
        } else if sender == scoreLabel {
            if UserData.instance.initialized {
                if (!Settings.instance.disableHighscoreShare && UserData.instance.shareScore > 0) && !Settings.instance.disableHighscoreShow {
                    handleShareOrShow()
                } else if !Settings.instance.disableHighscoreShare && UserData.instance.shareScore > 0 {
                    self.shareScore()
                } else if !Settings.instance.disableHighscoreShow {
                    self.showHighscore()
                }
            }
        } else if sender == matchingHistoryLabel {
            if shouldPerformSegue(withIdentifier: "history", sender: matchingHistoryLabel) {
                performSegue(withIdentifier: "history", sender: matchingHistoryLabel)
            }
        }
    }
    
    func handleShareOrShow() {
        let alertController = UIAlertController(title: "Highscore".localized, message: "ShareOrShowHighscore".aliasLocalized, preferredStyle: .alert)
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        
        if !Settings.instance.disableHighscoreShare && UserData.instance.shareScore > 0 {
            let shareAction = UIAlertAction(title: "Share Score".localized, style: .default, handler: { (action : UIAlertAction) in
                self.shareScore()
            })
            alertController.addAction(shareAction)
        }
        if !Settings.instance.disableHighscoreShow {
            let showAction = UIAlertAction(title: "Show Highscore".localized, style: .default, handler: { (action : UIAlertAction) in
                self.showHighscore()
            })
            alertController.addAction(showAction)
        }
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    func shareScore() {
        var alias = UserData.instance.firstName
        if !UserData.instance.status.isEmpty {
            alias += " (\(UserData.instance.status))"
        }
        let score = UserData.instance.shareScore
        let count = UserData.instance.scoreShareCount
        let title = score == 1 ? String(format: "ShareMatchingPoint".aliasLocalized, score) : String(format: "ShareMatchingPoints".aliasLocalized, score)
        let titleSuffix = count == 1 ? String(format: "ShareMatchingMatch".aliasLocalized, count) : String(format: "ShareMatchingMatches".aliasLocalized, count)
        let alertController = UIAlertController(title: "\(title) \(titleSuffix)", message: "ProvideAliasShare".aliasLocalized, preferredStyle: .alert)
        
        let okAction = UIAlertAction(title: "Share".localized, style: .default, handler: { (action : UIAlertAction) in
            let alias = alertController.textFields?.first?.text?.trimmingCharacters(in: .whitespacesAndNewlines)
            _ = DataService.instance.shareHighscore(alias: alias!, value: score, count: count).then { () -> () in
                let alertController = UIAlertController(title: "Highscore".localized, message: "ScoreShared".aliasLocalized, preferredStyle: .alert)
                let okAction = UIAlertAction(title: "OK".localized, style: .cancel, handler: nil)
                alertController.addAction(okAction)
                
                if !Settings.instance.disableHighscoreShow {
                    let showAction = UIAlertAction(title: "Show Highscore".localized, style: .default, handler: { (action : UIAlertAction) in
                        self.showHighscore()
                    })
                    alertController.addAction(showAction)
                }
                alertController.view?.tintColor = AccentColor
                self.present(alertController, animated: true, completion: nil)
            }.catch { (Error) -> () in
                let alertController = UIAlertController(title: "Network Error".localized, message: "UnexpectedErrorOccurred".aliasLocalized, preferredStyle: .alert)
                let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
                alertController.addAction(okAction)
                alertController.view?.tintColor = AccentColor
                self.present(alertController, animated: true, completion: nil)
            }
        })
        okAction.isEnabled = false
        alertController.addAction(okAction)
        okAlertAction = okAction
        
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        
        alertController.addTextField { (textField) in
            textField.autocorrectionType = .yes
            textField.spellCheckingType = .yes
            textField.autocapitalizationType = .sentences
            textField.placeholder = "Alias".localized
            textField.text = alias
            okAction.isEnabled = !alias.isEmpty
            NotificationCenter.default.addObserver(self, selector: #selector(self.handleTextFieldTextDidChangeNotification), name: NSNotification.Name.UITextFieldTextDidChange, object: textField)
        }
        
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    @objc func handleTextFieldTextDidChangeNotification(notification: Notification) {
        let textField = notification.object as! UITextField
        if let okAlertAction = okAlertAction {
            okAlertAction.isEnabled = !textField.text!.isEmpty
        }
    }
    
    func showHighscore() {
        if shouldPerformSegue(withIdentifier: "highscore", sender: scoreLabel) {
            self.performSegue(withIdentifier: "highscore", sender: scoreLabel)
        }
    }
    
    @IBAction func setupCancelPressed(_ sender: Any) {
        let firstNameTextIsFirstResponder = self.setupFirstNameTextField.isFirstResponder
        self.setupFirstNameTextField.resignFirstResponder()
        let alertController = UIAlertController(title: "First-time Configuration".localized, message: "SettingsConfiguration".aliasLocalized, preferredStyle: .alert)
        let backAction = UIAlertAction(title: "Back".localized, style: .default, handler: { (action : UIAlertAction) in
            if firstNameTextIsFirstResponder {
                self.setupFirstNameTextField.becomeFirstResponder()
            }
        })
        alertController.addAction(backAction)
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            self.setupEnd()
        })
        alertController.addAction(okAction)
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    func setupEnd() {
        setupFirstNameTextField.resignFirstResponder()
        UserData.instance.initialized = true
        UserData.instance.touch { () -> () in
            UIView.animate(withDuration: FadeTransition, animations: {
                self.setupContainerView.alpha = 0
                self.showContent()
            }, completion: { (completed) in
                self.setupContainerView.isHidden = true
                self.nameButton.isUserInteractionEnabled = true
                UIView.setAnimationsEnabled(false)
                self.navigationItem.leftBarButtonItems = self.leftBarButtonItems
                self.navigationItem.rightBarButtonItems = self.rightBarButtonItems
                UIView.setAnimationsEnabled(true)
            })
            NotificationCenter.default.post(name: SetupEndNotification, object: nil)
        }
    }
    
    @IBAction func setupNextPressed(_ sender: Any) {
        if setupPageControl.currentPage < setupPageControl.numberOfPages - 1 {
            setupPageControl.currentPage += 1
            updateScrollOffset()
            updatePageButtons()
        } else {
            UserData.instance.birthYear = year - setupBirthYearPickerView.selectedRow(inComponent: 0)
            self.setupEnd()
        }
    }

    @IBAction func pageChanged(_ sender: Any) {
        updateScrollOffset()
        updatePageButtons()
    }
    
    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate {
            updatePaging()
            updatePageButtons()
        }
    }

    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        updatePaging()
        updatePageButtons()
    }
    
    func updateScrollOffset() {
        let x = CGFloat(Float(setupPageControl.currentPage) * Float(setupScrollView.frame.size.width))
        setupScrollView.setContentOffset(CGPoint(x: x, y: 0), animated: true)
    }
    
    func updatePaging() {
        let pageNumber = Int(roundf(Float(setupScrollView.contentOffset.x) / Float(setupScrollView.frame.size.width)))
        setupPageControl.currentPage = pageNumber
    }
    
    func updatePageButtons() {
        if setupPageControl.currentPage == setupPageControl.numberOfPages - 1 {
            setupNextButton.setTitle("Finish".localized, for: .normal)
        } else {
            setupNextButton.setTitle("Next >".localized, for: .normal)
        }
        setupFirstNameTextField.resignFirstResponder()
    }
    
    
    @IBAction func firstNameEditingEnd(_ sender: Any) {
        UserData.instance.firstName = setupFirstNameTextField.text!
    }
    
    @IBAction func firstNameChanged(_ sender: Any) {
        UserData.instance.firstName = setupFirstNameTextField.text!
    }
    
    @IBAction func testGreetingPressed(_ sender: Any) {
        Voice.instance.sayHi(presenter: self, name: UserData.instance.firstName, gender: UserData.instance.gender, toast: true)
    }
    
    @IBAction func languageChanged(_ sender: Any) {
        switch setupLanguageSegmentedControl.selectedSegmentIndex {
        case 0:
            UserData.instance.langCode = "en"
        case 1:
            UserData.instance.langCode = "de"
        default:
            UserData.instance.langCode = "en"
        }
        setupOtherLanguageButton.setTitle("Other...".localized, for: .normal)
        NotificationCenter.default.post(name: UserDataLangChangedNotification, object: nil)
    }
    
    func didSelectLanguage(code: String) {
        UserData.instance.langCode = code        
        NotificationCenter.default.post(name: UserDataLangChangedNotification, object: nil)
    }
    
    @IBAction func genderChanged(_ sender: Any) {
        switch setupGenderSegmentedControl.selectedSegmentIndex {
        case 0:
            UserData.instance.gender = .male
        case 1:
            UserData.instance.gender = .female
        default:
            UserData.instance.gender = .none
        }
    }
    
    @IBAction func statusEditingEnd(_ sender: Any) {
        UserData.instance.status = statusTextField.text!
        UserData.instance.touch()
    }
    
    @IBAction func statusChanged(_ sender: Any) {
        UserData.instance.status = statusTextField.text!
    }
    
    @objc func didTap() {
        statusTextField.resignFirstResponder()
    }
    
    // MARK: TextField Delegate
    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == setupFirstNameTextField {
            guard let text = textField.text else { return true }
            let newLength = text.count + string.count - range.length
            return newLength <= NameMaxLength
        }
        return true
    }
    
    func textFieldShouldBeginEditing(_ textField: UITextField) -> Bool {
        if textField == statusTextField {
            textField.text = UserData.instance.status
        }
        return true
    }
    
    func textFieldDidEndEditing(_ textField: UITextField) {
        if textField == statusTextField {
            setStatusText()
        }
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
    
    // MARK: DatePicker DataSource
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return year - BaseYear
    }
    
    // MARK: DatePicker Delegate
    func pickerView(_ pickerView: UIPickerView, attributedTitleForRow row: Int, forComponent component: Int) -> NSAttributedString? {
        return NSAttributedString(string: String(year - row), attributes: [NSAttributedStringKey.foregroundColor:  AccentColor])
    }
    
    func pickerView(_ pickerView: UIPickerView,
                    didSelectRow row: Int,
                    inComponent component: Int) {
        UserData.instance.birthYear = year - row
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "sayhi",
            let profile = UserData.instance.currentProfile,
            let qrCodeViewController = (segue.destination as? UINavigationController)?.topViewController as? QRCodeViewController {
            qrCodeViewController.view.frame = self.view.bounds
            qrCodeViewController.profile = profile
        } else if segue.identifier == "language",
            let languageViewController = (segue.destination as? UINavigationController)?.topViewController as? SettingsLanguageTableController {
            languageViewController.navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: .cancel, target: languageViewController, action: #selector(languageViewController.done))
            languageViewController.selectedCode = UserData.instance.langCode
            languageViewController.delegate = self
            languageViewController.isPresentedModal = true
        } else if segue.identifier == "currentProfile",
            let tagViewController = segue.destination as? TagViewController,
            let profile = UserData.instance.currentProfile {
            tagViewController.profile = profile
        } else if segue.identifier == "history",
            let historyTableController = segue.destination as? HistoryTableController,
            let profile = UserData.instance.currentProfile {
            historyTableController.profile = profile
        }
    }
    
    override func shouldPerformSegue(withIdentifier identifier: String, sender: Any?) -> Bool {
        if ProtectedSegues.contains(identifier) {
            if !UserData.instance.initialized {
                UserData.instance.initialize { () -> () in
                    if UserData.instance.currentProfile != nil {
                        if identifier != "currentProfile" {
                            self.performSegue(withIdentifier: identifier, sender: sender)
                        }
                    }
                }
                return false
            } else if UserData.instance.currentProfile == nil {
                DispatchQueue.main.async() {
                    let alertController = UIAlertController(title: "Application Error".localized, message: "NoActiveProfile".aliasLocalized, preferredStyle: .alert)
                    let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
                    alertController.addAction(okAction)
                    alertController.view?.tintColor = AccentColor
                    self.present(alertController, animated: true, completion: nil)
                }
                return false
            }
        }
        return true
    }
}
