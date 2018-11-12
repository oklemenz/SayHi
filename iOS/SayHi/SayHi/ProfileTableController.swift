//
//  ProfileTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation

class ProfileTableController : ListViewController, RelationTypeTableControllerDelegate, UIPickerViewDelegate, UIPickerViewDataSource, UIGestureRecognizerDelegate {
    
    let year : Int = Calendar.current.dateComponents([.year], from: Date()).year!
    
    var okAlertAction : UIAlertAction?
    @IBOutlet weak var newProfileButton: UIBarButtonItem!
    
    @IBOutlet weak var helpEditMoreLabel: UILabel!
    @IBOutlet weak var helpCreateProfileArrow: UIImageView!
    @IBOutlet weak var helpCreateProfileLabel: UILabel!
    
    @IBAction func addPressed(_ sender: Any) {
        if UserData.instance.profiles.count > 0 && UserData.instance.birthYear < BaseYear {
            addEnterBirthYearAlert(completion: {
                self.showNewProfile()
            })
        } else {
            showNewProfile()
        }
    }
    
    func showNewProfile() {
        self.addProfile(completion: { (index: Int) in
            let indexPath = IndexPath(row: index, section: 0)
            self.tableView.insertRows(at: [indexPath], with: UITableViewRowAnimation.bottom)
            self.tableView.scrollToRow(at: indexPath, at: .top, animated: true)
        })
    }

    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.navigationItem.rightBarButtonItems!.append(self.editButtonItem)
        
        helpEditMoreLabel.text = String(format: "ProfileEdit".aliasLocalized, Emoji.relationType, Emoji.matchMode)
        
        if Settings.instance.disableNewProfiles {
            helpCreateProfileArrow.isHidden = true
            helpCreateProfileLabel.isHidden = true
            if let index = self.navigationItem.rightBarButtonItems?.index(of: newProfileButton) {
                self.navigationItem.rightBarButtonItems?.remove(at: index)
            }
        }
        
        enableRefresh(true)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        UserData.instance.sortProfiles()
        self.tableView.reloadData()
    }
        
    @objc override func refreshTriggered(_ sender: Any) {
        UserData.instance.sortProfiles()
        super.refreshTriggered(sender)
    }
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return UserData.instance.profiles.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "profileCell", for: indexPath)
        cell.selectionStyle = .default
        cell.selectedBackgroundView = CellSelectionImageView
        
        let profile = UserData.instance.profiles[indexPath.row]
        if profile.id == UserData.instance.currentProfileId {
            cell.imageView?.image = UIImage(named: "circle_checked")!.colored(AccentColor)
        } else {
            cell.imageView?.image = UIImage(named: "circle")!.colored(AccentColor)
        }
        
        cell.imageView?.isUserInteractionEnabled = true
        if (cell.imageView?.tag == 0) {
            let tapGesture = UILongPressGestureRecognizer(target: self, action: #selector(didPressIcon))
            tapGesture.minimumPressDuration = 0
            cell.imageView?.addGestureRecognizer(tapGesture)
            cell.imageView?.tag = profile.id
        }
        
        cell.accessoryType = .none
        cell.accessoryView = UIImageView(image: UIImage(named: "arrow_right"))
        
        let text = NSMutableAttributedString(string: profile.name)
        text.append(NSMutableAttributedString(
            string: "  \(dateTimeFormatter.string(from: profile.date))",
            attributes: [NSAttributedStringKey.foregroundColor: AccentColor,
                         NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
        cell.textLabel?.attributedText = text
        
        let detailText = NSMutableAttributedString()
        var separator = ""
        if profile.relationType != .none {
            detailText.append(NSMutableAttributedString(
                string: Emoji.relationType + profile.relationType.rawValue.codeLocalized,
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black]))
            separator = SeparatorString
        }
        
        if let matchMode = profile.matchMode {
            detailText.append(NSMutableAttributedString(
                string: separator + Emoji.matchMode + matchMode.description.codeLocalized,
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black]))
        }
        cell.detailTextLabel?.attributedText = detailText
        
        return cell
    }
    
    func profileRemove(_ indexPath: IndexPath, completion : ((Bool) -> ())? = nil ) {
        let profile = UserData.instance.profiles[indexPath.row]
        
        let alertController = UIAlertController(title: "Profile Deletion".localized, message: String(format: "DeleteProfile".aliasLocalized, profile.name), preferredStyle: .alert)
        let deleteAction = UIAlertAction(title: "Delete".localized, style: .destructive, handler: { (action : UIAlertAction) in
            if let index = UserData.instance.removeProfile(profile) {
                self.tableView.deleteRows(at: [IndexPath(row: index, section: 0)], with: .automatic)
                if let completion = completion {
                    completion(true)
                }
            }
        })
        alertController.addAction(deleteAction)
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: { (action : UIAlertAction) in
            self.isEditing = false
            if let completion = completion {
                completion(false)
            }
        })
        alertController.addAction(cancelAction)
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }

    func profileMore(_ indexPath: IndexPath, completion : ((Bool) -> ())? = nil ) {
        let profile = UserData.instance.profiles[indexPath.row]
        
        let alertController = UIAlertController(title: profile.name, message: "Select an Action".localized, preferredStyle: .actionSheet)
        
        let copyAction = UIAlertAction(title: "Copy Profile".localized, style: .default, handler: { (action : UIAlertAction) in
            self.isEditing = false
            self.copyProfile(profile, presenter: self, completion: { (index: Int) in
                self.tableView.insertRows(at: [IndexPath(row: index, section: 0)], with: UITableViewRowAnimation.bottom)
            })
        })
        alertController.addAction(copyAction)
        
        let renameAction = UIAlertAction(title: "Rename Profile".localized, style: .default, handler: { (action : UIAlertAction) in
            self.isEditing = false
            self.renameProfile(profile, completion: { (index: Int) in
                self.tableView.reloadRows(at: [IndexPath(row: index, section: 0)], with: UITableViewRowAnimation.fade)
            })
        })
        alertController.addAction(renameAction)
        
        let setRelationTypeAction = UIAlertAction(title: String(format: "SetRelationType".aliasLocalized, Emoji.relationType),
                                                  style: .default, handler: { (action : UIAlertAction) in
                                                    self.isEditing = false
                                                    let storyboard = UIStoryboard(name: "Main", bundle: nil)
                                                    let relationTypeNavController = storyboard.instantiateViewController(withIdentifier: "relationType") as! UINavigationController
                                                    let relationTypeController = relationTypeNavController.topViewController as! RelationTypeTableController
                                                    
                                                    relationTypeController.delegate = self
                                                    relationTypeController.relationType = profile.relationType
                                                    relationTypeController.contextIndexPath = indexPath
                                                    
                                                    self.present(relationTypeNavController, animated: true, completion: nil)
        })
        alertController.addAction(setRelationTypeAction)
        
        let overrideMatchModeAction = UIAlertAction(title: String(format: "OverrideMatchingMode".aliasLocalized, Emoji.matchMode),
                                                    style: .default, handler: { (action : UIAlertAction) in
                                                        self.isEditing = false
                                                        self.changeMatchingModeProfile(profile, presenter: self, indexPath: indexPath, completion: {
                                                            self.tableView.reloadRows(at: [indexPath], with: UITableViewRowAnimation.fade)
                                                        })
        })
        alertController.addAction(overrideMatchModeAction)
        
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: { (action : UIAlertAction) in
            self.isEditing = false
        })
        alertController.addAction(cancelAction)
        
        if isIPad() {
            let cell = self.tableView.cellForRow(at: indexPath)!
            alertController.popoverPresentationController?.sourceView = cell
            alertController.popoverPresentationController?.sourceRect = cell.bounds
        }
        
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
        
        if let completion = completion {
            completion(true)
        }
    }
    
    func profileSetActive(_ indexPath: IndexPath, completion : ((Bool) -> ())? = nil ) {
        let profile = UserData.instance.profiles[indexPath.row]
        UserData.instance.setCurrentProfile(profile)
        UserData.instance.touch()
        self.tableView.beginUpdates()
        self.tableView.reloadSections(IndexSet(integer: 0), with: .automatic)
        self.tableView.endUpdates()
        if let completion = completion {
            completion(true)
        }
    }
    
    @available(iOS 11.0, *)
    override func tableView(_ tableView: UITableView, leadingSwipeActionsConfigurationForRowAt indexPath: IndexPath) -> UISwipeActionsConfiguration? {
        let profile = UserData.instance.profiles[indexPath.row]
        
        var actions : [UIContextualAction] = []
        
        if UserData.instance.currentProfile !== profile {
            let activeAction = UIContextualAction(style: .normal, title: "Set Active".localized, handler: { (action, view, completion) in
                self.profileSetActive(indexPath, completion: completion)
            })
            activeAction.title = "Set Active".localized
            activeAction.image = UIImage(named: "circle_checked")
            activeAction.backgroundColor = AccentColor
            actions.append(activeAction)
        }
        
        return UISwipeActionsConfiguration(actions: actions)
    }
    
    @available(iOS 11.0, *)
    override func tableView(_ tableView: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath: IndexPath) -> UISwipeActionsConfiguration? {
        let profile = UserData.instance.profiles[indexPath.row]
        
        var actions : [UIContextualAction] = []
        
        if UserData.instance.profiles.count > 1 && profile.id != UserData.instance.currentProfileId {
            let deleteAction = UIContextualAction(style: .destructive, title: "Delete".localized, handler: { (action, view, completion) in
                self.profileRemove(indexPath, completion: completion)
            })
            actions.append(deleteAction)
        }
        
        let moreAction = UIContextualAction(style: .normal, title: "More...".localized, handler: { (action, view, completion) in
            self.profileMore(indexPath, completion: completion)
        })
        actions.append(moreAction)
        
        return UISwipeActionsConfiguration(actions: actions)
    }
    
    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        return true
    }
    
    override func tableView(_ tableView: UITableView, editActionsForRowAt indexPath: IndexPath) -> [UITableViewRowAction]? {
        let profile = UserData.instance.profiles[indexPath.row]
        
        var actions : [UITableViewRowAction] = []
        
        if UserData.instance.profiles.count > 1 && profile.id != UserData.instance.currentProfileId {
            let deleteAction = UITableViewRowAction(style: .destructive, title: "Delete".localized, handler:{action, indexpath in
                self.profileRemove(indexPath)
            })
            actions.append(deleteAction)
        }
        
        let moreAction = UITableViewRowAction(style: .normal, title: "More...".localized, handler:{action, indexpath in
            self.profileMore(indexPath)
        })
        actions.append(moreAction)

        return actions
    }
    
    var refAlpha: CGFloat = 1.0
    
    @objc func didPressIcon(_ gesture : UILongPressGestureRecognizer) {
        let PressedAlphaRatio: CGFloat = 2.5
        let iconView = gesture.view!
        if gesture.state == .began {
            iconView.alpha = refAlpha / PressedAlphaRatio
        } else if gesture.state == .ended {
            UIView.animate(withDuration: 0.2, animations: {
                iconView.alpha = self.refAlpha
            })
            let location = gesture.location(in: iconView)
            if iconView.bounds.contains(location) {
                let profileId = gesture.view!.tag
                if let profile = UserData.instance.getProfile(profileId) {
                    UserData.instance.setCurrentProfile(profile)
                    UserData.instance.touch()
                    self.tableView.reloadData()
                }
            }
        } else if gesture.state == .cancelled {
            UIView.animate(withDuration: 0.2, animations: {
                iconView.alpha = self.refAlpha
            })
        }
    }
    
    func didSelectRelationType(_ relationType: RelationType, at indexPath: IndexPath) {
        let profile = UserData.instance.profiles[indexPath.row]
        UserData.instance.changeRelationType(profile, relationType: relationType)
        self.tableView.reloadRows(at: [indexPath], with: UITableViewRowAnimation.fade)
    }
    
    func addEnterBirthYearAlert(completion : (() -> ())? = nil ) {
        let title = "ActivateMultipleProfiles".aliasLocalized
        let message = "\("ProvideBirthYear".aliasLocalized)\n\n\n\n\n"
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alertController.isModalInPopover = true
        
        let pickerFrame = CGRect(x: 0, y: 65, width: 270, height: 90)
        let pickerView = UIPickerView(frame: pickerFrame)
        pickerView.delegate = self
        pickerView.dataSource = self
        
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            UserData.instance.birthYear = self.year - pickerView.selectedRow(inComponent: 0)
            if let completion = completion {
                completion()
            }
        })
        alertController.addAction(okAction)

        let skipAction = UIAlertAction(title: "Skip Once".localized, style: .default, handler: { (action : UIAlertAction) in
            UserData.instance.birthYear = 0
            if let completion = completion {
                completion()
            }
        })
        alertController.addAction(skipAction)
        
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler:  { (action : UIAlertAction) in
            UserData.instance.birthYear = 0
        })
        alertController.addAction(cancelAction)
        
        alertController.view.addSubview(pickerView)
        
        alertController.view.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)

        let width = NSLayoutConstraint(item: alertController.view, attribute: NSLayoutAttribute.width, relatedBy: NSLayoutRelation.equal, toItem: nil, attribute: NSLayoutAttribute.notAnAttribute, multiplier: 1, constant: 270)
        alertController.view.addConstraint(width)
        let height = NSLayoutConstraint(item: alertController.view, attribute: NSLayoutAttribute.height, relatedBy: NSLayoutRelation.equal, toItem: nil, attribute: NSLayoutAttribute.notAnAttribute, multiplier: 1, constant: 295)
        alertController.view.addConstraint(height)
        
        pickerView.selectRow(30, inComponent: 0, animated: false)
    }
    
    func addProfile(completion : ((Int) -> ())? = nil ) {
        setEditing(false, animated: true)
        
        let alertController = UIAlertController(title: "New Profile".localized, message: "Enter name for this Profile.".localized, preferredStyle: .alert)
        
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            let name = alertController.textFields?.first?.text?.trimmingCharacters(in: .whitespacesAndNewlines)
            let index = UserData.instance.addProfile(name: name!)
            DispatchQueue.main.async {
                if let completion = completion {
                    completion(index)
                }
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
            textField.placeholder = "Name".localized
            NotificationCenter.default.addObserver(self, selector: #selector(self.handleTextFieldTextDidChangeNotification), name: NSNotification.Name.UITextFieldTextDidChange, object: textField)
        }
        
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    func renameProfile(_ profile: Profile, completion : ((Int) -> ())? = nil ) {
        let alertController = UIAlertController(title: "Rename Profile".localized, message: "Provide Profile Name.".localized, preferredStyle: .alert)
        
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            let name = alertController.textFields?.first?.text?.trimmingCharacters(in: .whitespacesAndNewlines)
            let index = UserData.instance.renameProfile(profile, name: name!)
            DispatchQueue.main.async {
                if let completion = completion {
                    completion(index)
                }
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
            textField.text = profile.name
            textField.placeholder = "Name".localized
            NotificationCenter.default.addObserver(self, selector: #selector(self.handleTextFieldTextDidChangeNotification), name: NSNotification.Name.UITextFieldTextDidChange, object: textField)
        }
        
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    func copyProfile(_ profile: Profile, presenter : UIViewController, completion : ((Int) -> ())? = nil ) {
        let alertController = UIAlertController(title: "Copy Profile".localized, message: "Enter name for new Profile.".localized, preferredStyle: .alert)
        
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            let name = alertController.textFields?.first?.text?.trimmingCharacters(in: .whitespacesAndNewlines)
            let index = UserData.instance.copyProfile(profile, name: name!)
            DispatchQueue.main.async {
                if let completion = completion {
                    completion(index)
                }
            }
        })
        okAction.isEnabled = true
        alertController.addAction(okAction)
        okAlertAction = okAction
        
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        
        alertController.addTextField { (textField) in
            textField.autocorrectionType = .yes
            textField.spellCheckingType = .yes
            textField.autocapitalizationType = .sentences
            textField.text = String(format: "%@*".localized, profile.name)
            textField.placeholder = "Name".localized
            NotificationCenter.default.addObserver(self, selector: #selector(self.handleTextFieldTextDidChangeNotification), name: NSNotification.Name.UITextFieldTextDidChange, object: textField)
        }
        
        alertController.view?.tintColor = AccentColor
        presenter.present(alertController, animated: true, completion: nil)
    }
    
    func changeMatchingModeProfile(_ profile: Profile, presenter : UIViewController, indexPath: IndexPath, completion : (() -> ())? = nil ) {
        let alertController = UIAlertController(title: "Override Matching Mode".localized, message: "Choose custom profile matching mode.".localized, preferredStyle: .actionSheet)
        
        let handleActionResult = { (matchMode : MatchMode?) in
            UserData.instance.changeMatchingMode(profile, matchMode: matchMode)
            DispatchQueue.main.async {
                if let completion = completion {
                    completion()
                }
            }
        }
        
        let defaultSettingAction = UIAlertAction(title: "Default Setting".localized, style: .default, handler:  { (action : UIAlertAction) in
            handleActionResult(nil)
        })
        alertController.addAction(defaultSettingAction)
        let basicAction = UIAlertAction(title: MatchMode.basic.numberedDescription.codeLocalized, style: .default, handler:  { (action : UIAlertAction) in
            handleActionResult(.basic)
        })
        alertController.addAction(basicAction)
        let exactAction = UIAlertAction(title: MatchMode.exact.numberedDescription.codeLocalized, style: .default, handler:  { (action : UIAlertAction) in
            handleActionResult(.exact)
        })
        alertController.addAction(exactAction)
        let adaptAction = UIAlertAction(title: MatchMode.adapt.numberedDescription.codeLocalized, style: .default, handler:  { (action : UIAlertAction) in
            handleActionResult(.adapt)
        })
        alertController.addAction(adaptAction)
        let tryAction = UIAlertAction(title: MatchMode.tries.numberedDescription.codeLocalized, style: .default, handler:  { (action : UIAlertAction) in
            handleActionResult(.tries)
        })
        alertController.addAction(tryAction)
        let openAction = UIAlertAction(title: MatchMode.open.numberedDescription.codeLocalized, style: .default, handler:  { (action : UIAlertAction) in
            handleActionResult(.open)
        })
        alertController.addAction(openAction)
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        
        if isIPad() {
            let cell = self.tableView.cellForRow(at: indexPath)!
            alertController.popoverPresentationController?.sourceView = cell
            alertController.popoverPresentationController?.sourceRect = cell.bounds
        }
        
        alertController.view?.tintColor = AccentColor
        presenter.present(alertController, animated: true, completion: nil)
    }
    
    @objc func handleTextFieldTextDidChangeNotification(notification: Notification) {
        let textField = notification.object as! UITextField
        if let okAlertAction = okAlertAction {
            okAlertAction.isEnabled = !textField.text!.isEmpty
        }
    }
    
    //MARK: DatePicker DataSource
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return year - BaseYear
    }
    
    //MARK: DatePicker Delegate
    func pickerView(_ pickerView: UIPickerView, attributedTitleForRow row: Int, forComponent component: Int) -> NSAttributedString? {
        return NSAttributedString(string: String(year - row), attributes: [NSAttributedStringKey.foregroundColor:AccentColor])
    }
    
    func pickerView(_ pickerView: UIPickerView,
                    didSelectRow row: Int,
                    inComponent component: Int){
        UserData.instance.birthYear = year - row
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "profile",
            let tagViewController = segue.destination as? TagViewController {
            let indexPath = self.tableView.indexPathForSelectedRow!
            let profile = UserData.instance.profiles[indexPath.row]
            tagViewController.profile = profile
        }
    }
}
