//
//  SearchCategoryViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 16.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

protocol SearchCategoryViewControllerDelegate: class {
    func didSelectCategory(_ category: Category, newCategory: NewCategory?)
}

class SearchCategoryViewController: ListViewController, UISearchBarDelegate, NewCategoryTableControllerDelegate {

    @IBOutlet weak var searchBar: UISearchBar!
    @IBOutlet weak var newCategoryButton: UIBarButtonItem!
    
    @IBOutlet weak var helpNewCategorArrow: UIImageView!
    @IBOutlet weak var helpNewCategoryLabel: UILabel!
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }

    var query: CategoryQuery = CategoryQuery()
    var categories: [Category] = []
    
    weak var delegate: SearchCategoryViewControllerDelegate?
    
    var keyboardShownInitial: Bool = false
    var _isPresentedModal: Bool = false
    var isPresentedModal: Bool {
        set {
            _isPresentedModal = newValue
            self.navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Close".localized, style: .done, target: self, action: #selector(hide))
            self.navigationItem.rightBarButtonItems = []
        }
        get {
            return _isPresentedModal
        }
    }
    
    var selectedCategoryKey: String?
    var primaryLangCategory: Category?
    
    var _contentLangCode: String = UserData.instance.langCode
    var contentLangCode: String {
        set {
            _contentLangCode = newValue
            query.langCode = contentLangCode
            
            let text = NSMutableAttributedString(
                string: "Search Category".localized + "\n",
                attributes: [NSAttributedString.Key.foregroundColor: AccentColor])
            text.append(NSMutableAttributedString(
                string: Locale.current.localizedString(forIdentifier: contentLangCode) ?? "",
                attributes: [NSAttributedString.Key.foregroundColor: UIColor.black,
                             NSAttributedString.Key.font: UIFont.systemFont(ofSize: 12.0)]))
            let label = UILabel(frame: CGRect(x:0, y:0, width:200, height:50))
            label.backgroundColor = UIColor.clear
            label.numberOfLines = 2
            label.font = UIFont.boldSystemFont(ofSize: 16.0)
            label.textAlignment = .center
            label.textColor = UIColor.white
            label.attributedText = text
            self.navigationItem.titleView = label
        }
        get {
            return _contentLangCode
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        query.search = true
        query.searchText = ""
        searchBar.text = query.searchText

        self.activityIndicatorTop = 64
        self.activityIndicator = true
        
        if Settings.instance.disableNewCategories {
            helpNewCategorArrow.isHidden = true
            helpNewCategoryLabel.isHidden = true
            if newCategoryButton != nil {
                if let index = self.navigationItem.rightBarButtonItems?.firstIndex(of: newCategoryButton) {
                    self.navigationItem.rightBarButtonItems?.remove(at: index)
                }
            }
        }
        
        self.refresh()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if !keyboardShownInitial {
            searchBar.becomeFirstResponder()
            keyboardShownInitial = true
        }
    }
    
    @objc func hide() {
        searchBar.resignFirstResponder()
        if isPresentedModal {
            self.dismiss(animated: true, completion: nil)
        } else {
            _ = self.navigationController?.popViewController(animated: true)
        }
    }
    
    func didCreateNewCategory(_ newCategory: NewCategory) {
        let category = newCategory.category
        self.delegate?.didSelectCategory(category, newCategory: newCategory)
        self.hide()
    }
    
    func didUseExistingCategory(_ category: Category, newCategory: NewCategory) {
        self.delegate?.didSelectCategory(category, newCategory: nil)
        self.hide()
    }
    
    func refresh() {
        fetchCategories()
    }
    
    func clearCategories() {
        self.categories = []
        self.tableView.reloadData()
    }
    
    func fetchCategories() {
        clearCategories()
        self.startActivityIndicator()
        _ = DataService.instance.fetchCategories(query).then { (categories) -> () in
            self.categories = categories
            self.tableView.reloadData()
            self.stopActivityIndicator()
        }
    }
    
    // MARK: TableView
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return categories.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "categoryCell", for: indexPath) as! CategoryTableCell
        
        cell.selectionStyle = .default
        
        let category = categories[indexPath.row]
        category.selected = category.key == selectedCategoryKey
        
        cell.accessoryType = .none
        if category.key == selectedCategoryKey {
            cell.accessoryView = UIImageView(image: UIImage(named: "checkmark")?.colored(AccentColor))
        } else {
            cell.accessoryView = nil
        }

        cell.selectedBackgroundView = CellSelectionImageView
        cell.tintColor = category.textColor
        cell.data = category

        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        super.tableView(tableView, didSelectRowAt: indexPath)
        let category = categories[indexPath.row]
        category.selected = true
        self.delegate?.didSelectCategory(category, newCategory: nil)
        self.hide()
    }
        
    // MARK: Search
    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {
        self.query.searchText = searchText
        self.refresh()
    }
    
    func searchBarCancelButtonClicked(_ searchBar: UISearchBar) {
        cancelSearching()
    }
    
    func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
        self.view.endEditing(true)
    }
    
    func searchBarTextDidBeginEditing(_ searchBar: UISearchBar) {
        self.searchBar!.setShowsCancelButton(true, animated: true)
    }
    
    func searchBarTextDidEndEditing(_ searchBar: UISearchBar) {
        self.searchBar!.setShowsCancelButton(false, animated: false)
    }
    
    func cancelSearching(){
        self.searchBar!.resignFirstResponder()
    }

    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "addCategory",
            let newCategoryTableController = (segue.destination as? UINavigationController)?.topViewController as? NewCategoryTableController {
            newCategoryTableController.delegate = self
            newCategoryTableController.contentLangCode = contentLangCode
            if let category = primaryLangCategory {
                _ = newCategoryTableController.view
                newCategoryTableController.setPrimaryLangCategory(category)
            }
        }
    }
}
