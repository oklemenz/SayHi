//
//  SimilarCategoryTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.01.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

protocol SimilarCategoryTableControllerDelegate: class {
    func didUseOwnCategory()
    func didUseSimilarCategory(_ category: Category)
}

class SimilarCategoryTableController: ListViewController {
    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    @IBAction func didPressUseOwn(_ sender: Any) {
        self.delegate?.didUseOwnCategory()
        self.hide()
    }
    
    weak var delegate: SimilarCategoryTableControllerDelegate?
    
    var _categories: [Category] = []
    var categories: [Category]! {
        set {
            self.startActivityIndicator()
            _ = DataService.instance.completeCategories(newValue).then { (categories: [Category]) -> Void in
                self._categories = newValue
                self.tableView.reloadData()
                self.stopActivityIndicator()
            }
        }
        get {
            return _categories
        }
    }
    
    var _contentLangCode: String = UserData.instance.langCode
    var contentLangCode: String {
        set {
            _contentLangCode = newValue
            
            let text = NSMutableAttributedString(
                string: "Similar Categories".localized + "\n",
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
        }
        get {
            return _contentLangCode
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.activityIndicator = true
    }
    
    func hide() {
        _ = self.navigationController?.popViewController(animated: true)
    }
    
    // MARK: TableView
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return categories.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "similarCategoryCell", for: indexPath) as! SimilarCategoryTableCell
        cell.selectionStyle = .default

        let category = categories[indexPath.row]
        cell.selectedBackgroundView = CellSelectionImageView
        cell.tintColor = category.textColor
        cell.data = category
        
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        super.tableView(tableView, didSelectRowAt: indexPath)
        let category = categories[indexPath.row]
        self.delegate?.didUseSimilarCategory(category)
        self.hide()
    }
}
