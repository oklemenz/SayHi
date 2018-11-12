//
//  NewTagTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

let TagMaxWidthFromScreenBias:CGFloat = 100

protocol NewTagTableControllerDelegate: class {
    func didCreateNewTag(_ newTag: NewTag)
    func didUseExistingTag(_ tag: Tag, newTag: NewTag)
}

class NewTagTableController: ListViewController, UITextFieldDelegate, SearchCategoryViewControllerDelegate, NewTagTableControllerDelegate, SimilarTagTableControllerDelegate {
    
    var timer: Timer?
    var submitted: Bool = false
    var checking: Bool = false
    var cancelled: Bool = false
    var similarTags: [Tag] = []
    
    @IBOutlet weak var assignmentSegmentedControl: UISegmentedControl!
    @IBAction func assignmentChanged(_ sender: Any) {
        newTag.assignPos = assignmentSegmentedControl.selectedSegmentIndex == 0
    }

    @IBOutlet weak var languageDetailLabel: UILabel!
    
    @IBOutlet weak var nameTextField: UITextField!

    @IBAction func nameEditingEnd(_ sender: Any) {
        newTag.name = nameTextField.text!
        self.checkDoneActive()
    }
    
    @IBAction func nameChanged(_ sender: Any) {
        newTag.name = nameTextField.text!
        doneButtonItem.isEnabled = false
    }
    
    @IBOutlet weak var categoryDetailLabel: UILabel!
    @IBOutlet weak var categoryCell: UITableViewCell!
    
    @IBOutlet weak var primaryLanguageDetailLabel: UILabel!
    @IBOutlet weak var primaryLangTagDetailLabel: UILabel!
    @IBOutlet weak var primaryLangTagCell: UITableViewCell!
    
    @IBOutlet weak var primaryLangCategoryDetailLabel: UILabel!
    @IBOutlet weak var primaryLangCategoryCell: SelectionTableCell!
    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    @IBOutlet weak var helpAssignmentLabel: UILabel!
    @IBOutlet weak var helpAssignmentArrow: UIImageView!
    @IBOutlet weak var helpEnterNameArrow: UIImageView!
    @IBOutlet weak var helpEnterNameLabel: UILabel!
    @IBOutlet weak var helpSelectCategroyArrow: UIImageView!
    @IBOutlet weak var helpSelectCategoryLabel: UILabel!
    @IBOutlet weak var helpRefPrimaryLangTagLabel: UILabel!
    @IBOutlet weak var helpRefPrimaryLangTagArrow: UIImageView!
    
    @IBOutlet weak var helpEnterNameArrowTopConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpEnterNameLabelTopConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpSelectCategoryLabelTopConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpSelectCategoryArrowTopConstraint: NSLayoutConstraint!
    
    @IBAction func donePressed(_ sender: Any) {
        self.submitted = true
        timer?.invalidate()
        self.doneButtonItem.isEnabled = false
        self.cancelButtonItem.isEnabled = false
        nameTextField.resignFirstResponder()
        self.checkSpelling {
            self.checkSimilarTag {
                self.createTag()
            }
        }
    }
    
    @IBAction func cancelPressed(_ sender: Any) {
        self.cancelled = true
        hide()
    }
    
    func hide() {
        nameTextField.resignFirstResponder()
        if primaryMode {
            _ = self.navigationController?.popViewController(animated: true)
        } else {
            self.dismiss(animated: true, completion: nil)
        }
    }

    var keyboardShownInitial: Bool = false
    var doneButtonItem: UIBarButtonItem!
    var cancelButtonItem: UIBarButtonItem!
    weak var delegate : NewTagTableControllerDelegate?
    
    var primaryMode : Bool = false
    
    var hidePrimaryReferencePrimaryLang : Bool {
        return contentLangCode == PrimaryLangCode || !Settings.instance.primaryReference
    }
    
    var hidePrimaryReferencePrimaryMode : Bool {
        return primaryMode || !Settings.instance.primaryReference
    }
    
    var _contentLangCode: String = UserData.instance.langCode
    var contentLangCode: String {
        set {
            _contentLangCode = newValue
            newTag.langCode = contentLangCode
            
            let text = NSMutableAttributedString(
                string: "New Tag".localized + "\n",
                attributes: [NSAttributedStringKey.foregroundColor: AccentColor])
            text.append(NSMutableAttributedString(
                string: Locale.current.localizedString(forIdentifier: contentLangCode) ?? "",
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                             NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
            let label = UILabel(frame: CGRect(x:0, y:0, width:200, height:50))
            label.backgroundColor = UIColor.clear
            label.numberOfLines = 2
            label.font = UIFont.boldSystemFont(ofSize: 16.0)
            label.textAlignment = .center
            label.textColor = UIColor.white
            label.attributedText = text
            self.navigationItem.titleView = label
            
            helpRefPrimaryLangTagLabel.isHidden = hidePrimaryReferencePrimaryLang
            helpRefPrimaryLangTagArrow.isHidden = hidePrimaryReferencePrimaryLang

            if primaryMode {
                helpAssignmentLabel.isHidden = true
                helpAssignmentArrow.isHidden = true
                helpEnterNameArrowTopConstraint.constant -= 118
                helpEnterNameLabelTopConstraint.constant -= 118
                helpSelectCategoryArrowTopConstraint.constant -= 118
                helpSelectCategoryLabelTopConstraint.constant -= 118
            }
        }
        
        get {
            return _contentLangCode
        }
    }
    
    var categoryView : CategoryView?
    var primaryLangTagView : TagView?
    var primaryLangCategoryView : CategoryView?
    
    let newTag = NewTag()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        doneButtonItem = self.navigationItem.leftBarButtonItem!
        cancelButtonItem = self.navigationItem.rightBarButtonItem!
        
        languageDetailLabel.text = Locale.current.localizedString(forIdentifier: contentLangCode)
        primaryLanguageDetailLabel.text = Locale.current.localizedString(forIdentifier: PrimaryLangCode)
        primaryLangTagCell.accessoryType = .none
        primaryLangTagCell.accessoryView = UIImageView(image: UIImage(named: "plus"))
        
        if let _ = Settings.instance.leftLabel {
            assignmentSegmentedControl.setTitle(Emoji.like, forSegmentAt: 0)
        } else {
            assignmentSegmentedControl.setTitle("ILike".termLocalized(Emoji.like), forSegmentAt: 0)
        }
        if let _ = Settings.instance.rightLabel {
            assignmentSegmentedControl.setTitle(Emoji.dislike, forSegmentAt: 1)
        } else {
            assignmentSegmentedControl.setTitle("IDislike".termLocalized(Emoji.dislike), forSegmentAt: 1)
        }
        
        if newTag.category != nil {
            setCategory(newTag.category!)
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.checkDoneActive()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if !keyboardShownInitial {
            nameTextField.becomeFirstResponder()
            keyboardShownInitial = true
        }
    }
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        var sections = super.numberOfSections(in: tableView)
        if hidePrimaryReferencePrimaryLang {
            sections -= 1
        }
        return sections
    }
    
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if section == 0 && self.primaryMode {
            return nil
        }
        return super.tableView(tableView, titleForHeaderInSection: section)
    }
    
    override func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        if section == 0 && self.primaryMode {
            return CGFloat.leastNonzeroMagnitude
        }
        return super.tableView(tableView, heightForHeaderInSection: section)
    }
    
    override func tableView(_ tableView: UITableView, titleForFooterInSection section: Int) -> String? {
        if section == 0 && self.primaryMode {
            return nil
        }
        return super.tableView(tableView, titleForFooterInSection: section)
    }
    
    override func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        if section == 0 && self.primaryMode {
            return CGFloat.leastNonzeroMagnitude
        }
        return super.tableView(tableView, heightForFooterInSection: section)
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if section == 0 && self.primaryMode {
            return 0
        }
        return super.tableView(tableView, numberOfRowsInSection: section)
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
        nameTextField.resignFirstResponder()
        
        let cell = tableView.cellForRow(at: indexPath)
        if cell == primaryLangTagCell {
            let storyboard = UIStoryboard(name: "Main", bundle: nil)
            let newTagController = storyboard.instantiateViewController(withIdentifier: "addTag") as! NewTagTableController
            newTagController.delegate = self
            newTagController.primaryMode = true
            newTagController.contentLangCode = PrimaryLangCode
            newTagController.newTag.category = newTag.primaryLangCategory
            newTagController.newTag.categoryNew = newTag.categoryNew
            self.navigationController?.pushViewController(newTagController, animated: true)
        }
    }
    
    func checkSpelling(_ continueCreate: (() -> Void)? = nil) {
        let textChecker = UITextChecker()
        let misspelledRange = textChecker.rangeOfMisspelledWord(in: nameTextField.text!,
                                                                range: NSRange(0..<nameTextField.text!.count),
                                                                startingAt: 0,
                                                                wrap: false,
                                                                language: contentLangCode)
        if misspelledRange.location == NSNotFound {
            if let continueCreate = continueCreate {
                continueCreate()
            }
        } else {
            let alertController = UIAlertController(title: "Word Spelling Check".localized, message: "MisspelledWordsDetected".aliasLocalized, preferredStyle: .alert)
            let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
                if let continueCreate = continueCreate {
                    continueCreate()
                }
            })
            alertController.addAction(okAction)
            let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: { (action : UIAlertAction) in
                self.submitCancel()
                self.nameTextField.becomeFirstResponder()
            })
            alertController.addAction(cancelAction)
            alertController.view?.tintColor = AccentColor
            self.present(alertController, animated: true, completion: nil)
        }
    }
    
    func createTag() {
        _ = DataService.instance.createTag(newTag).then { (tag) -> Void in
            self.delegate?.didCreateNewTag(self.newTag)
            self.hide()
        }.catch { (error) in
            self.doneButtonItem.isEnabled = true
            self.submitCancel()
            
            let alertController = UIAlertController(title: "Network Error".localized, message: "UnexpectedErrorOccurred".aliasLocalized, preferredStyle: .alert)
            let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
            alertController.addAction(okAction)
            alertController.view?.tintColor = AccentColor
            self.present(alertController, animated: true, completion: nil)
        }
    }
    
    func didSelectCategory(_ category: Category, newCategory: NewCategory?) {
        if !hidePrimaryReferencePrimaryMode && !(category.primaryLangKey == newTag.primaryLangCategory?.key ?? "") {
            newTag.primaryLangTag?.categoryKey = ""
            primaryLangTagView?.isHidden = true
            primaryLangTagDetailLabel.isHidden = false
            
            newTag.primaryLangCategory = nil
            primaryLangCategoryView?.isHidden = true
            primaryLangCategoryDetailLabel.isHidden = false
        }
        self.newTag.categoryNew = newCategory != nil
        if let newPrimaryLangCategory = newCategory?.primaryLangCategory {
            category.deriveFrom(newPrimaryLangCategory)
            self.setPrimaryLangCategory(newPrimaryLangCategory)
        }
        self.setCategory(category)
    }
    
    func setCategory(_ category: Category) {
        category.selected = true
        newTag.category = category
        
        if !hidePrimaryReferencePrimaryMode && !hidePrimaryReferencePrimaryLang && !category.primaryLangKey.isEmpty && newTag.primaryLangCategory == nil {
            _ = DataService.instance.getCategory(key: category.primaryLangKey, langCode: PrimaryLangCode).then(execute: { (category) -> () in
                if let category = category {
                    self.setPrimaryLangCategory(category)
                }
            })
        }
        
        categoryDetailLabel.isHidden = true
        if categoryView == nil {
            categoryView = Bundle.main.loadNibNamed("CategoryView", owner: nil, options: nil)?.first as? CategoryView
            categoryView!.translatesAutoresizingMaskIntoConstraints = false
            categoryView!.setMaxWidthScreenWithBias(CategoryMaxWidthFromScreenBias)
            categoryCell.contentView.addSubview(categoryView!)
            categoryCell.contentView.addConstraint(NSLayoutConstraint(
                item: categoryView!,
                attribute: .centerY,
                relatedBy: .equal,
                toItem: categoryCell.contentView,
                attribute: .centerY,
                multiplier: 1.0,
                constant: 0))
            categoryView!.addConstraint(NSLayoutConstraint(
                item: categoryView!,
                attribute: .height,
                relatedBy: .equal,
                toItem: nil,
                attribute: .notAnAttribute,
                multiplier: 1.0,
                constant: 25))
            categoryCell.contentView.addConstraint(NSLayoutConstraint(
                item: categoryView!,
                attribute: .trailingMargin,
                relatedBy: .equal,
                toItem: categoryCell.contentView,
                attribute: .trailingMargin,
                multiplier: 1.0,
                constant: 0))
            categoryView!.data = category
            categoryView!.isUserInteractionEnabled = false
        } else {
            categoryView!.isHidden = false
            categoryView!.data = category
        }
        
        self.checkDoneActive()
    }
    
    func setPrimaryLangCategory(_ category: Category) {
        category.selected = true
        newTag.primaryLangCategory = category
        
        primaryLangCategoryDetailLabel.isHidden = true
        if primaryLangCategoryView == nil {
            primaryLangCategoryView = Bundle.main.loadNibNamed("CategoryView", owner: nil, options: nil)?.first as? CategoryView
            primaryLangCategoryView!.translatesAutoresizingMaskIntoConstraints = false
            primaryLangCategoryView!.setMaxWidthScreenWithBias(CategoryMaxWidthFromScreenBias)
            primaryLangCategoryCell.contentView.addSubview(primaryLangCategoryView!)
            primaryLangCategoryCell.contentView.addConstraint(NSLayoutConstraint(
                item: primaryLangCategoryView!,
                attribute: .centerY,
                relatedBy: .equal,
                toItem: primaryLangCategoryCell.contentView,
                attribute: .centerY,
                multiplier: 1.0,
                constant: 0))
            primaryLangCategoryView!.addConstraint(NSLayoutConstraint(
                item: primaryLangCategoryView!,
                attribute: .height,
                relatedBy: .equal,
                toItem: nil,
                attribute: .notAnAttribute,
                multiplier: 1.0,
                constant: 25))
            primaryLangCategoryCell.contentView.addConstraint(NSLayoutConstraint(
                item: primaryLangCategoryView!,
                attribute: .trailingMargin,
                relatedBy: .equal,
                toItem: primaryLangCategoryCell.contentView,
                attribute: .trailingMargin,
                multiplier: 1.0,
                constant: -15))
            primaryLangCategoryView!.data = category
            primaryLangCategoryView!.isUserInteractionEnabled = false
        } else {
            primaryLangCategoryView!.isHidden = false
            primaryLangCategoryView!.data = category
        }
    }
    
    func didCreateNewTag(_ newTag: NewTag) {
        self.setPrimaryLangTag(newTag.tag)
        self.newTag.categoryNew = newTag.categoryNew
    }
    
    func didUseExistingTag(_ tag: Tag, newTag: NewTag) {
        self.setPrimaryLangTag(tag)
    }
    
    func setPrimaryLangTag(_ tag: Tag) {
        if newTag.primaryLangCategory?.key != tag.categoryKey {
            newTag.category = nil
            categoryView?.isHidden = true
            categoryDetailLabel.isHidden = false
        }

        newTag.primaryLangTag = nil
        primaryLangTagView?.isHidden = true
        primaryLangTagDetailLabel.isHidden = false
        
        newTag.primaryLangCategory = nil
        primaryLangCategoryView?.isHidden = true
        primaryLangCategoryDetailLabel.isHidden = false
        
        tag.selected = true
        tag.category?.selected = true
        
        newTag.primaryLangTag = tag
        newTag.primaryLangCategory = tag.category
        
        if let category = tag.category {
            self.setPrimaryLangCategory(category)
        }
        if !hidePrimaryReferencePrimaryMode && !hidePrimaryReferencePrimaryLang && !tag.categoryKey.isEmpty && newTag.category == nil {
            var query = CategoryQuery()
            query.primaryLangKey = tag.categoryKey
            query.langCode = contentLangCode
            _ = DataService.instance.fetchCategories(query).then { (categories) -> () in
                if categories.count > 0 {
                    self.setCategory(categories[0])
                }
            }
        }
        
        updatePrimaryLangTag(tag)
    }
    
    func updatePrimaryLangTag(_ tag: Tag) {
        primaryLangTagDetailLabel.isHidden = true
        primaryLangTagCell.accessoryView = UIImageView(image: UIImage(named: "arrow_right"))
        if primaryLangTagView == nil {
            primaryLangTagView = Bundle.main.loadNibNamed("TagView", owner: nil, options: nil)?.first as? TagView
            primaryLangTagView!.translatesAutoresizingMaskIntoConstraints = false
            primaryLangTagView!.setMaxWidthScreenWithBias(TagMaxWidthFromScreenBias)
            primaryLangTagCell.contentView.addSubview(primaryLangTagView!)
            primaryLangTagCell.contentView.addConstraint(NSLayoutConstraint(
                item: primaryLangTagView!,
                attribute: .centerY,
                relatedBy: .equal,
                toItem: primaryLangTagCell.contentView,
                attribute: .centerY,
                multiplier: 1.0,
                constant: 0))
            primaryLangTagView!.addConstraint(NSLayoutConstraint(
                item: primaryLangTagView!,
                attribute: .height,
                relatedBy: .equal,
                toItem: nil,
                attribute: .notAnAttribute,
                multiplier: 1.0,
                constant: 25))
            primaryLangTagCell.contentView.addConstraint(NSLayoutConstraint(
                item: primaryLangTagView!,
                attribute: .trailingMargin,
                relatedBy: .equal,
                toItem: primaryLangTagCell.contentView,
                attribute: .trailingMargin,
                multiplier: 1.0,
                constant: 0))
            primaryLangTagView!.data = tag
            primaryLangTagView!.isUserInteractionEnabled = false
        } else {
            primaryLangTagView!.isHidden = false
            primaryLangTagView!.data = tag
        }
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        nameTextField.resignFirstResponder()
        return true
    }
    
    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if textField == nameTextField {
            doneButtonItem.isEnabled = false
            guard let text = textField.text else { return true }
            let newLength = text.count + string.count - range.length
            let ok = newLength <= NameMaxLength
            if ok {
                timer?.invalidate()
                timer = Timer.scheduledTimer(timeInterval: 0.5, target: self, selector: #selector(checkDoneActive), userInfo: nil, repeats: false)
            }
            return ok
        }
        return true
    }
    
    @objc func checkDoneActive() {
        self.doneButtonItem.isEnabled = false
        timer?.invalidate()
        if self.submitted || self.checking || self.cancelled {
            return
        }
        if !newTag.name.isEmpty && newTag.category != nil && (hidePrimaryReferencePrimaryLang || newTag.primaryLangTag != nil) {
            self.checking = true
            _ = DataService.instance.fetchSimilarTags(name: newTag.name, langCode: contentLangCode).then { (tags) -> () in
                self.similarTags = tags
                self.doneButtonItem.isEnabled = true
                self.checking = false
            }
        }
    }
    
    func checkSimilarTag(_ createNew: (() -> Void)? = nil) {
        if similarTags.count > 0 {
            var alertController: UIAlertController!
            if let tag = findSameActiveTag() {
                alertController = UIAlertController(title: "Tag Creation".localized, message: String(format: "TagAlreadyExists".aliasLocalized, self.newTag.name, self.newTag.category?.name ?? ""), preferredStyle: .alert)
                let useExistingAction = UIAlertAction(title: "UseExistingTag".aliasLocalized, style: .default, handler: { (action : UIAlertAction) in
                    self.didUseSimilarTag(tag)
                })
                alertController.addAction(useExistingAction)
            } else if let _ = findSameStageTag() {
                if let createNew = createNew {
                    createNew()
                }
                return
            } else {
                alertController = UIAlertController(title: "Tag Creation".localized, message: "SimilarTagsExist".aliasLocalized, preferredStyle: .alert)
                let showSimilar = UIAlertAction(title: "Show Similar".localized, style: .default, handler: { (action : UIAlertAction) in
                    self.doneButtonItem.isEnabled = true
                    self.submitCancel()
                    self.performSegue(withIdentifier: "similarTag", sender: nil)
                })
                alertController.addAction(showSimilar)
                let ignoreAction = UIAlertAction(title: "Continue".localized, style: .default, handler: { (action : UIAlertAction) in
                    if let createNew = createNew {
                        createNew()
                    }
                })
                alertController.addAction(ignoreAction)
            }
            let resumeAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: { (action : UIAlertAction) in
                self.doneButtonItem.isEnabled = true
                self.submitCancel()
                self.nameTextField.becomeFirstResponder()
            })
            alertController.addAction(resumeAction)
            alertController.view?.tintColor = AccentColor
            self.present(alertController, animated: true, completion: nil)
        } else {
            if let createNew = createNew {
                createNew()
            }
        }
    }
    
    func findSameActiveTag() -> Tag? {
        for tag in similarTags {
            if !tag.stage && tag.name == self.newTag.name && tag.categoryKey == self.newTag.category?.key &&
                tag.primaryLangKey == self.newTag.primaryLangTag?.key ?? "" {
                return tag
            }
        }
        return nil
    }
    
    func findSameStageTag() -> Tag? {
        for tag in similarTags {
            if tag.stage && tag.name == self.newTag.name && tag.categoryKey == self.newTag.category?.key &&
                tag.primaryLangKey == self.newTag.primaryLangTag?.key ?? "" {
                return tag
            }
        }
        return nil
    }
    
    func didUseOwnTag() {
        createTag()
    }
    
    func didUseSimilarTag(_ tag: Tag) {
        _ = DataService.instance.completeTag(tag).then { tag -> Void in
            self.delegate?.didUseExistingTag(tag, newTag: self.newTag)
            self.hide()
        }
    }
    
    func submitCancel() {
        self.submitted = false
        self.cancelButtonItem.isEnabled = true
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "searchCategory",
            let searchCategoryController = segue.destination as? SearchCategoryViewController {
            searchCategoryController.delegate = self
            searchCategoryController.contentLangCode = contentLangCode
            searchCategoryController.selectedCategoryKey = self.newTag.category?.key
            if newTag.categoryNew {
                searchCategoryController.primaryLangCategory = self.newTag.primaryLangCategory
            }
        } else if segue.identifier == "similarTag",
            let similarTagController = segue.destination as? SimilarTagTableController {
            similarTagController.delegate = self
            similarTagController.tags = self.similarTags
            similarTagController.contentLangCode = self.contentLangCode
        }
    }
}
