
//
//  SettingsController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class SettingsViewController: ListViewController, UITextFieldDelegate, UIPickerViewDelegate, UIPickerViewDataSource, SettingsLanguageTableControllerDelegate {
    
    let year : Int = Calendar.current.dateComponents([.year], from: Date()).year!
    
    let CellHeight : CGFloat = 44
    let PickerCellHeight : CGFloat = 162
    
    let YearPickerIndexPath = IndexPath(row: 2, section: 0)
    let MatchModePickerIndexPath = IndexPath(row: 5, section: 1)
    let RequestAuthPickerIndexPath = IndexPath(row: 3, section: 2)
    
    let matchingDescription = [
        "MatchBasic".termLocalized(Emoji.like),
        "MatchExact".termLocalized(Emoji.like, Emoji.dislike),
        "MatchAdapt".termLocalized(Emoji.like, Emoji.dislike),
        "MatchTry".termLocalized(Emoji.dislike, Emoji.like),
        "MatchOpen".termLocalized(Emoji.like, Emoji.dislike)
    ]
    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    @IBOutlet weak var firstNameTextField: UITextField!

    @IBAction func firstNameTextChanged(_ sender: Any) {
        UserData.instance.firstName = firstNameTextField.text!
    }
    
    @IBOutlet weak var birthYearCell: UITableViewCell!
    @IBOutlet weak var birthYearValueLabel: UILabel!
    @IBOutlet weak var birthYearPickerView: UIPickerView!
    @IBOutlet weak var birthYearPickerViewCell: UITableViewCell!
    var birthYearPickerViewCellVisible : Bool = false
    
    @IBOutlet weak var genderSegmentedControl: UISegmentedControl!
    @IBAction func genderChanged(_ sender: Any) {
        resignFirstResponders()
        switch genderSegmentedControl.selectedSegmentIndex {
        case 0:
            UserData.instance.gender = .none
        case 1:
            UserData.instance.gender = .male
        case 2:
            UserData.instance.gender = .female
        default:
            UserData.instance.gender = .none
        }
    }
    
    @IBOutlet weak var languageValueLabel: UILabel!
    func didSelectLanguage(code: String) {
        UserData.instance.langCode = code
        languageValueLabel.text = Locale.current.localizedString(forIdentifier: code)
        NotificationCenter.default.post(name: UserDataLangChangedNotification, object: nil)
        NoTagAlertShown = false
    }
    @IBOutlet weak var languageCell: UITableViewCell!
    
    @IBOutlet weak var statusTextField: UITextField!
    
    @IBAction func statusTextChanged(_ sender: Any) {
        UserData.instance.status = statusTextField.text!
    }
    
    @IBOutlet weak var matchGreetingPlaySwitch: UISwitch!
    @IBAction func matchGreetingPlayChanged(_ sender: Any) {
        resignFirstResponders()
        UserData.instance.matchPlayGreeting = matchGreetingPlaySwitch.isOn
    }
    
    @IBOutlet weak var matchVibrateSwitch: UISwitch!
    @IBAction func matchVibrateChanged(_ sender: Any) {
        resignFirstResponders()
        UserData.instance.matchVibrate = matchVibrateSwitch.isOn
    }
    
    @IBOutlet weak var matchRecordGreetingCell: UITableViewCell!
    @IBOutlet weak var matchRecordGreetingValueLabel: UILabel!
    
    
    @IBOutlet weak var matchHandshakeSwitch: UISwitch!
    @IBAction func matchHandshakeChanged(_ sender: Any) {
        resignFirstResponders()
        UserData.instance.matchHandshake = matchHandshakeSwitch.isOn
    }

    @IBOutlet weak var matchModeCell: UITableViewCell!
    @IBOutlet weak var matchModeLabel: UILabel!
    @IBOutlet weak var matchModeValueLabel: UILabel!
    @IBOutlet weak var matchModePickerView: UIPickerView!
    @IBOutlet weak var matchModePickerViewCell: UITableViewCell!
    var matchModePickerViewCellVisible : Bool = false
    
    @IBOutlet weak var spaceCell: UITableViewCell!
    @IBOutlet weak var spaceValueLabel: UILabel!
    
    @IBOutlet weak var touchIDLabel: UILabel!
    @IBOutlet weak var touchIDSwitch: UISwitch!
    @IBAction func touchIDChanged(_ sender: Any) {
        resignFirstResponders()
        UserData.instance.touchID = touchIDSwitch.isOn
    }
    
    @IBOutlet weak var requestAuthCell: UITableViewCell!
    @IBOutlet weak var requestAuthLabel: UILabel!
    @IBOutlet weak var requestAuthValueLabel: UILabel!
    @IBOutlet weak var requestAuthPickerView: UIPickerView!
    @IBOutlet weak var requestAuthPickerViewCell: UITableViewCell!
    var requestAuthPickerViewCellVisible : Bool = false

    @IBOutlet weak var writeInvitationMailCell: UITableViewCell!
    @IBOutlet weak var sendInvitationMessageCell: UITableViewCell!
    @IBOutlet weak var rateAppCell: UITableViewCell!
    @IBOutlet weak var supportCell: UITableViewCell!

    var okAlertAction : UIAlertAction?
    
    @IBAction func donePressed(_ sender: Any) {
        close()
    }
    
    func close() {
        UserData.instance.touch()
        resignFirstResponders()
        self.dismiss(animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        autoDeselectCell = false
        firstNameTextField.text = UserData.instance.firstName
        updateBirthYear()
        if UserData.instance.birthYear >= BaseYear {
            birthYearPickerView.selectRow(year - UserData.instance.birthYear, inComponent: 0, animated: false)
        } else {
            birthYearPickerView.selectRow(30, inComponent: 0, animated: false)
        }
        
        switch UserData.instance.gender {
            case .none:
                genderSegmentedControl.selectedSegmentIndex = 0
            break
            case .male:
                genderSegmentedControl.selectedSegmentIndex = 1
            break
            case .female:
                genderSegmentedControl.selectedSegmentIndex = 2
            break
        }
        
        languageValueLabel.text = Locale.current.localizedString(forIdentifier: UserData.instance.langCode)
        if Settings.instance.disableSettingsLanguage {
            languageCell.accessoryType = .none
        }
        
        statusTextField.text = UserData.instance.status
        
        if vibrateEnabled() {
            matchVibrateSwitch.setOn(UserData.instance.matchVibrate, animated: false)
        } else {
            matchVibrateSwitch.setOn(false, animated: false)
            matchVibrateSwitch.isEnabled = false
        }

        matchGreetingPlaySwitch.setOn(UserData.instance.matchPlayGreeting, animated: false)
        matchHandshakeSwitch.setOn(UserData.instance.matchHandshake, animated: false)
        if Settings.instance.disableSettingsHandshake {
            matchHandshakeSwitch.isEnabled = false
        }
        
        matchModePickerView.selectRow(MatchMode.list.firstIndex(of: UserData.instance.matchMode) ?? 0, inComponent: 0, animated: false)
        updateMatchMode()

        updateRecordMatchGreeting()
        
        if SecureStore.space == StandardSpace {
            spaceValueLabel.text = StandardSpace.localized
        } else {
            spaceValueLabel.text = SecureStore.space
        }
        
        matchModeLabel.text = String(format: "ModeIcon".aliasLocalized, Emoji.matchMode)
        
        touchIDSwitch.setOn(UserData.instance.touchID, animated: false)
        if biometricsEnabled() {
            touchIDLabel.text = biometricsText()
        } else {
            touchIDLabel.text = "Use Passcode".localized
            if !passcodeEnabled() {
                touchIDSwitch.setOn(false, animated: false)
                touchIDSwitch.isEnabled = false
            }
        }
        requestAuthPickerView.selectRow(PasscodeTimeout.list.firstIndex(of: UserData.instance.passcodeTimeout) ?? 0, inComponent: 0, animated: false)
        updateRequestAuth()
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = super.tableView(tableView, cellForRowAt: indexPath)
        cell.selectionStyle = .default
        cell.selectedBackgroundView = CellSelectionImageView
        

        if cell.accessoryType == .disclosureIndicator {
            cell.accessoryView = UIImageView(image: UIImage(named: "arrow_right"))
        }
        cell.accessoryType = .none
        
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        super.tableView(tableView, didSelectRowAt: indexPath)
        resignFirstResponders()
        
        let cell = tableView.cellForRow(at: indexPath)
        
        if cell == birthYearCell || cell == requestAuthCell {
            self.tableView.deselectRow(at: indexPath, animated: false)
        } else {
            self.tableView.deselectRow(at: indexPath, animated: true)
        }
        
        if cell == birthYearCell {
            birthYearPickerViewCellVisible = !birthYearPickerViewCellVisible
            tableView.beginUpdates()
            tableView.endUpdates()
        } else if cell == spaceCell {
            switchSpace()
        } else if cell == matchModeCell {
            if !Settings.instance.disableSettingsMatchMode {
                matchModePickerViewCellVisible = !matchModePickerViewCellVisible
                tableView.beginUpdates()
                tableView.endUpdates()
                scrollForce = true
                let rowRect = tableView.rectForRow(at: MatchModePickerIndexPath)
                tableView.scrollRectToVisible(rowRect, animated: true)
            }
        } else if cell == requestAuthCell {
            requestAuthPickerViewCellVisible = !requestAuthPickerViewCellVisible
            tableView.beginUpdates()
            tableView.endUpdates()
            scrollForce = true
            let rowRect = tableView.rectForRow(at: RequestAuthPickerIndexPath)
            tableView.scrollRectToVisible(rowRect, animated: true)
        } else if cell == matchRecordGreetingCell {
            recordMatchGreeting(matchRecordGreetingCell)
        } else if cell == writeInvitationMailCell {
            fadingLocked = true
            Mail.instance.sendInviteFriendsMail(presenter: self)
        } else if cell == sendInvitationMessageCell {
            fadingLocked = true
            Mail.instance.sendInviteFriendsMessage(presenter: self)
        } else if cell == supportCell {
            Mail.instance.sendSupportMail(presenter: self)
        } else if cell == rateAppCell {
            AppDelegate.instance.openAppInStore()
            Analytics.instance.logRateApp()
        }
    }
    
    override func tableView(_ tableView: UITableView, titleForFooterInSection section: Int) -> String? {
        if section == 1 {
            var footer = matchingDescription[UserData.instance.matchMode.rawValue - 1]
            if !footer.isEmpty {
                footer += "\n\n"
            }
            if Settings.instance.disableSettingsMatchMode {
                footer += "MatchForce".aliasLocalized
            } else {
                footer += "MatchExplanation".aliasLocalized
            }
            return footer
        }
        return super.tableView(tableView, titleForFooterInSection: section)
    }
    
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        if indexPath == YearPickerIndexPath {
            return birthYearPickerViewCellVisible ? PickerCellHeight : 0
        } else if indexPath == MatchModePickerIndexPath {
            return matchModePickerViewCellVisible ? PickerCellHeight : 0
        } else if indexPath == RequestAuthPickerIndexPath {
            return requestAuthPickerViewCellVisible ? PickerCellHeight : 0
        }
        return CellHeight
    }
    
    //MARK: DatePicker DataSource
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        if pickerView == birthYearPickerView {
            return 1
        } else if pickerView == matchModePickerView {
            return 1
        } else if pickerView == requestAuthPickerView {
            return 1
        }
        return 0
    }

    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        if pickerView == birthYearPickerView {
            return year - BaseYear + 1
        } else if pickerView == matchModePickerView {
            return MatchMode.list.count
        } else if pickerView == requestAuthPickerView {
            return PasscodeTimeout.list.count
        }
        return 0
    }
    
    //MARK: DatePicker Delegate
    func pickerView(_ pickerView: UIPickerView, attributedTitleForRow row: Int, forComponent component: Int) -> NSAttributedString? {
        if pickerView == birthYearPickerView {
            return NSAttributedString(string: String(year - row), attributes: [NSAttributedString.Key.foregroundColor:UIColor.white])
        } else if pickerView == matchModePickerView {
            return NSAttributedString(string: MatchMode.list[row].numberedDescription.codeLocalized, attributes: [NSAttributedString.Key.foregroundColor:UIColor.white])
        } else if pickerView == requestAuthPickerView {
            return NSAttributedString(string: "\(PasscodeTimeout.list[row])".codeLocalized, attributes: [NSAttributedString.Key.foregroundColor:UIColor.white])
        }
        return nil
    }
    
    func pickerView(_ pickerView: UIPickerView,
                             didSelectRow row: Int,
                             inComponent component: Int) {
        if pickerView == birthYearPickerView {
            UserData.instance.birthYear = year - row
            updateBirthYear()
        } else if pickerView == matchModePickerView {
            UserData.instance.matchMode = MatchMode.list[row]
            updateMatchMode()
            self.tableView.reloadData()
        } else if pickerView == requestAuthPickerView {
            UserData.instance.passcodeTimeout = PasscodeTimeout.list[row]
            updateRequestAuth()
        }
        resignFirstResponders()
    }
    
    func updateBirthYear() {
        if UserData.instance.birthYear >= BaseYear {
            if UserData.instance.age == 1 {
                birthYearValueLabel.text = String(format: "%i (~%i year)".localized, UserData.instance.birthYear, UserData.instance.age)
            } else {
                birthYearValueLabel.text = String(format: "%i (~%i years)".localized, UserData.instance.birthYear, UserData.instance.age)
            }
        } else {
            birthYearValueLabel.text = "n/a".localized
        }
    }
    
    func updateMatchMode() {
        matchModeValueLabel.text = "\(UserData.instance.matchMode)".codeLocalized
    }
    
    func updateRequestAuth() {
        requestAuthValueLabel.text = "\(UserData.instance.passcodeTimeout)".codeLocalized
    }
    
    func recordMatchGreeting(_ cell: UITableViewCell) {
        let alertController = UIAlertController(title: "Tag Matching".localized, message: "Matching Voice".localized, preferredStyle: .actionSheet)
        let recordAction = UIAlertAction(title: "Record New".localized, style: .default, handler:  { (action : UIAlertAction) in
            let message = String(format: "Hi, I'm %@!".localized, UserData.instance.firstName)
            Voice.instance.record(presenter: self, message: message, completion: { (voiceData) in
                if let voiceData = voiceData {
                    let rerecord = UserData.instance.greetingVoice != nil
                    UserData.instance.greetingVoice = voiceData
                    self.updateRecordMatchGreeting()
                    if !rerecord {
                        Analytics.instance.logRecordVoice()
                    } else {
                        Analytics.instance.logRerecordVoice()
                    }
                }
            })
        })
        alertController.addAction(recordAction)
        if let messageVoiceData = UserData.instance.greetingVoice {
            let playAction = UIAlertAction(title: "Play Recorded Voice".localized, style: .default, handler: { (action : UIAlertAction) in
                _ = Voice.instance.replay(presenter: self, voice: messageVoiceData)
            })
            alertController.addAction(playAction)
            let clearAction = UIAlertAction(title: "Clear Recorded Voice".localized, style: .default, handler: { (action : UIAlertAction) in
                UserData.instance.greetingVoice = nil
                self.updateRecordMatchGreeting()
                Analytics.instance.logRemoveRecordedVoice()
            })
            alertController.addAction(clearAction)
        } else {
            let playAction = UIAlertAction(title: "Play Synthesized Voice".localized, style: .default, handler: { (action : UIAlertAction) in
                Voice.instance.sayHi(presenter: self, name: UserData.instance.firstName, gender: UserData.instance.gender, toast: true)
            })
            alertController.addAction(playAction)
        }
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        
        if isIPad() {
            alertController.popoverPresentationController?.sourceView = cell
            alertController.popoverPresentationController?.sourceRect = cell.bounds
        }
        
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    func updateRecordMatchGreeting() {
        matchRecordGreetingValueLabel.text = UserData.instance.greetingVoice != nil ? "Recorded".localized : "Synthesized".localized
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        resignFirstResponders()
        return true
    }
    
    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == firstNameTextField {
            guard let text = textField.text else { return true }
            let newLength = text.count + string.count - range.length
            return newLength <= NameMaxLength
        } else if textField == statusTextField {
            guard let text = textField.text else { return true }
            let newLength = text.count + string.count - range.length
            return newLength <= StatusMaxLength
        }
        return true
    }
    
    func resignFirstResponders() {
        firstNameTextField.resignFirstResponder()
        statusTextField.resignFirstResponder()
    }
    
    func switchSpace() {
        let alertController = UIAlertController(title: "Space Switch".localized, message: "Enter a space name or choose standard space.".localized, preferredStyle: .alert)
        
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            let space = alertController.textFields!.first!.text!.trimmingCharacters(in: .whitespacesAndNewlines)
            let spaceRefName = space.lowercased()
            if spaceRefName == SecureStore.spaceRefName {
                return
            } else if spaceRefName == StandardSpace.lowercased() {
                self.performSpaceSwitch()
            } else if !spaceRefName.isEmpty {
                _ = DataService.instance.fetchSpaceMeta(space: spaceRefName).then(execute: { (meta) -> Void in
                    if let meta = meta {
                        if meta["protected"] as? Bool == true {
                            let alertController = UIAlertController(title: "Space Switch".localized, message: "Enter access code of space.".localized, preferredStyle: .alert)
                            
                            let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
                                let accessCode = alertController.textFields!.first!.text!
                                let accessCodeHash = Crypto.hash(accessCode)
                                _ = DataService.instance.verifySpaceProtection(space: spaceRefName, accessCode: accessCodeHash).then(execute: { (verified) -> Void in
                                    if verified {
                                        self.performSpaceSwitch(space)
                                    } else {
                                        let alertController = UIAlertController(title: "Space Switch".localized, message: "Access code not correct.".localized, preferredStyle: .alert)
                                        
                                        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
                                        })
                                        alertController.addAction(okAction)
                                        
                                        alertController.view?.tintColor = AccentColor
                                        self.present(alertController, animated: true, completion: nil)
                                    }
                                })
                            })
                            okAction.isEnabled = false
                            alertController.addAction(okAction)
                            self.okAlertAction = okAction
                            
                            let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
                            alertController.addAction(cancelAction)
                            
                            alertController.addTextField { (textField) in
                                textField.autocorrectionType = .no
                                textField.spellCheckingType = .no
                                textField.autocapitalizationType = .none
                                textField.isSecureTextEntry = true
                                textField.text = ""
                                textField.placeholder = "Access Code".localized
                                NotificationCenter.default.addObserver(self, selector: #selector(self.handleTextFieldTextDidChangeNotification), name: UITextField.textDidChangeNotification, object: textField)
                            }
                            
                            alertController.view?.tintColor = AccentColor
                            self.present(alertController, animated: true, completion: nil)
                        } else {
                            self.performSpaceSwitch(space)
                        }
                    } else {
                        let alertController = UIAlertController(title: "Space Switch".localized, message: String(format: "Space '%@' not found.".localized, space), preferredStyle: .alert)
                        
                        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
                        })
                        alertController.addAction(okAction)
                        
                        alertController.view?.tintColor = AccentColor
                        self.present(alertController, animated: true, completion: nil)
                    }
                })
            }
        })
        okAction.isEnabled = false
        alertController.addAction(okAction)
        okAlertAction = okAction
        
        let defaultAction = UIAlertAction(title: StandardSpace.localized, style: .default, handler: { (action : UIAlertAction) in
            if SecureStore.spaceRefName == StandardSpace.lowercased() {
                return
            }
            self.performSpaceSwitch()
        })
        alertController.addAction(defaultAction)
        
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        
        alertController.addTextField { (textField) in
            textField.autocorrectionType = .no
            textField.spellCheckingType = .no
            textField.autocapitalizationType = .none
            textField.text = ""
            textField.placeholder = "Name".localized
            NotificationCenter.default.addObserver(self, selector: #selector(self.handleTextFieldTextDidChangeNotification), name: UITextField.textDidChangeNotification, object: textField)
        }
        
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    func performSpaceSwitch(_ space: String? = nil) {
        UserData.instance.touch {
            if let space = space {
                SecureStore.switchSpace(space)
                self.spaceValueLabel.text = space
            } else {
                SecureStore.switchToStandardSpace()
                self.spaceValueLabel.text = StandardSpace.localized
            }
            self.close()
        }
    }
    
    @objc func handleTextFieldTextDidChangeNotification(notification: Notification) {
        let textField = notification.object as! UITextField
        if let okAlertAction = okAlertAction {
            okAlertAction.isEnabled = !textField.text!.isEmpty
        }
    }
    
    override func shouldPerformSegue(withIdentifier identifier: String, sender: Any?) -> Bool {
        if identifier == "language" {
            if Settings.instance.disableSettingsLanguage {
                return false
            }
        }
        return true
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "language",
            let languageViewController = segue.destination as? SettingsLanguageTableController {
            languageViewController.selectedCode = UserData.instance.langCode
            languageViewController.delegate = self
        }
    }
}
