//
//  NewCategoryTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 16.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

let CategoryMaxWidthFromScreenBias: CGFloat = 140

protocol NewCategoryTableControllerDelegate: class {
    func didCreateNewCategory(_ newCategory: NewCategory)
    func didUseExistingCategory(_ category: Category, newCategory: NewCategory)
}

class NewCategoryTableController: ListViewController, UITextFieldDelegate, NewCategoryTableControllerDelegate, SimilarCategoryTableControllerDelegate {

    var timer: Timer?
    var submitted: Bool = false
    var checking: Bool = false
    var cancelled: Bool = false
    var similarCategories: [Category] = []

    @IBOutlet weak var languageDetailLabel: UILabel!
    
    @IBOutlet weak var nameTextField: UITextField!

    @IBAction func nameEditingEnd(_ sender: Any) {
        newCategory.name = nameTextField.text!
        self.checkDoneActive()
    }

    @IBAction func nameChanged(_ sender: Any) {
        newCategory.name = nameTextField.text!
        doneButtonItem.isEnabled = false
    }
    
    @IBOutlet weak var primaryLanguageDetailLabel: UILabel!
    @IBOutlet weak var primaryLangCategoryDetailLabel: UILabel!
    @IBOutlet weak var primaryLangCategoryCell: UITableViewCell!
    
    @IBOutlet weak var helpRefPrimaryLangCategoryLabel: UILabel!
    @IBOutlet weak var helpRefPrimaryLangCategoryArrow: UIImageView!
    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }

    @IBAction func donePressed(_ sender: Any) {
        self.submitted = true
        timer?.invalidate()
        self.doneButtonItem.isEnabled = false
        self.cancelButtonItem.isEnabled = false
        nameTextField.resignFirstResponder()
        self.checkSpelling {
            self.checkSimilarCategory {
                self.createCategory()
            }
        }
    }
    
    @IBAction func cancelPressed(_ sender: Any) {
        self.cancelled = true
        hide()
    }

    var keyboardShownInitial: Bool = false
    var doneButtonItem: UIBarButtonItem!
    var cancelButtonItem: UIBarButtonItem!
    weak var delegate : NewCategoryTableControllerDelegate?
    
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
            newCategory.langCode = contentLangCode
            
            let text = NSMutableAttributedString(
                string: "New Category".localized + "\n",
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
            
            helpRefPrimaryLangCategoryLabel.isHidden = hidePrimaryReferencePrimaryLang
            helpRefPrimaryLangCategoryArrow.isHidden = hidePrimaryReferencePrimaryLang
        }
        get {
            return _contentLangCode
        }
    }
    
    var primaryLangCategoryView : CategoryView?
    let newCategory = NewCategory()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        doneButtonItem = self.navigationItem.leftBarButtonItem!
        cancelButtonItem = self.navigationItem.rightBarButtonItem!
        
        languageDetailLabel.text = Locale.current.localizedString(forIdentifier: contentLangCode)
        primaryLanguageDetailLabel.text = Locale.current.localizedString(forIdentifier: PrimaryLangCode)
        
        primaryLangCategoryCell.accessoryType = .none
        primaryLangCategoryCell.accessoryView = UIImageView(image: UIImage(named: "plus"))
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
        let sections = super.numberOfSections(in: tableView)
        if hidePrimaryReferencePrimaryLang {
            return sections - 1
        }
        return sections
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
        if cell == primaryLangCategoryCell {
            let storyboard = UIStoryboard(name: "Main", bundle: nil)
            let newCategoryController = storyboard.instantiateViewController(withIdentifier: "addCategory") as! NewCategoryTableController
            newCategoryController.delegate = self
            newCategoryController.primaryMode = true
            newCategoryController.contentLangCode = PrimaryLangCode
            self.navigationController?.pushViewController(newCategoryController, animated: true)
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
    
    func createCategory() {
        _ = DataService.instance.createCategory(newCategory).then { (category) -> Void in
            self.delegate?.didCreateNewCategory(self.newCategory)
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

    func hide() {
        nameTextField.resignFirstResponder()
        if primaryMode {
            _ = self.navigationController?.popViewController(animated: true)
        } else {
            self.dismiss(animated: true, completion: nil)
        }
    }
    
    func didCreateNewCategory(_ newCategory: NewCategory) {
        self.setPrimaryLangCategory(newCategory.category)
    }
    
    func didUseExistingCategory(_ category: Category, newCategory: NewCategory) {
        self.setPrimaryLangCategory(category)
    }
    
    func setPrimaryLangCategory(_ category: Category) {
        category.selected = true
        newCategory.primaryLangCategory = category
        
        primaryLangCategoryDetailLabel.isHidden = true
        primaryLangCategoryCell.accessoryView = UIImageView(image: UIImage(named: "arrow_right"))
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
                constant: 0))
            primaryLangCategoryView!.data = category
            primaryLangCategoryView!.isUserInteractionEnabled = false
        } else {
            primaryLangCategoryView!.data = category
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
        if !newCategory.name.isEmpty && (hidePrimaryReferencePrimaryLang || newCategory.primaryLangCategory != nil) {
            self.checking = true
            _ = DataService.instance.fetchSimilarCategories(name: newCategory.name, langCode: contentLangCode).then { (categories) -> () in
                self.similarCategories = categories
                self.doneButtonItem.isEnabled = true
                self.checking = false
            }
        }
    }
    
    func checkSimilarCategory(_ createNew: (() -> Void)? = nil) {
        if similarCategories.count > 0 {
            var alertController: UIAlertController!
            if let category = findSameActiveCategory() {
                alertController = UIAlertController(title: "Category Creation".localized, message: String(format: "CategoryAlreadyExists".aliasLocalized, self.newCategory.name), preferredStyle: .alert)
                let useExistingAction = UIAlertAction(title: "UseExistingCategory".aliasLocalized, style: .default, handler: { (action : UIAlertAction) in
                    self.didUseSimilarCategory(category)
                })
                alertController.addAction(useExistingAction)
            } else if let _ = findSameStageCategory() {
                if let createNew = createNew {
                    createNew()
                }
                return
            } else {
                alertController = UIAlertController(title: "Category Creation".localized, message: "SimilarCategoriesExist".aliasLocalized, preferredStyle: .alert)
                let showSimilar = UIAlertAction(title: "Show Similar".localized, style: .default, handler: { (action : UIAlertAction) in
                    self.doneButtonItem.isEnabled = true
                    self.submitCancel()
                    self.performSegue(withIdentifier: "similarCategory", sender: nil)
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
    
    func findSameActiveCategory() -> Category? {
        for category in similarCategories {
            if !category.stage && category.name == self.newCategory.name && category.primaryLangKey == self.newCategory.primaryLangCategory?.key ?? "" {
                return category
            }
        }
        return nil
    }
    
    func findSameStageCategory() -> Category? {
        for category in similarCategories {
            if category.stage && category.name == self.newCategory.name && category.primaryLangKey == self.newCategory.primaryLangCategory?.key ?? "" {
                return category
            }
        }
        return nil
    }
    
    func didUseOwnCategory() {
        self.createCategory()
    }
    
    func didUseSimilarCategory(_ category: Category) {
        _ = DataService.instance.completeCategory(category).then { tag -> Void in
            self.delegate?.didUseExistingCategory(category, newCategory: self.newCategory)
            self.hide()
        }
    }
    
    func submitCancel() {
        self.submitted = false
        self.cancelButtonItem.isEnabled = true
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "similarCategory",
            let similarCategoryController = segue.destination as? SimilarCategoryTableController {
            similarCategoryController.delegate = self
            similarCategoryController.categories = self.similarCategories
            similarCategoryController.contentLangCode = self.contentLangCode
        }
    }
}
