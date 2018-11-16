//
//  SimilarTagTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.01.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

protocol SimilarTagTableControllerDelegate: class {
    func didUseOwnTag()
    func didUseSimilarTag(_ tag: Tag)
}

class SimilarTagTableController: ListViewController {
    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    @IBAction func didPressUseOwn(_ sender: Any) {
        self.delegate?.didUseOwnTag()
        self.hide()
    }
    
    weak var delegate: SimilarTagTableControllerDelegate?
    
    var _tags: [Tag] = []
    var tags: [Tag]! {
        set {
            self.startActivityIndicator()
            _ = DataService.instance.completeTags(newValue).then { (tags: [Tag]) -> Void in
                self._tags = newValue
                self.tableView.reloadData()
                self.stopActivityIndicator()
            }
        }
        get {
            return _tags
        }
    }
    
    var _contentLangCode: String = UserData.instance.langCode
    var contentLangCode: String {
        set {
            _contentLangCode = newValue
            
            let text = NSMutableAttributedString(
                string: "Similar Tags".localized + "\n",
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
        self.activityIndicator = true
    }
    
    func hide() {
        _ = self.navigationController?.popViewController(animated: true)
    }
    
    // MARK: TableView
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 80
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tags.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "similarTagCell", for: indexPath) as! SimilarTagTableCell
        cell.selectionStyle = .default
        
        let tag = tags[indexPath.row]
        cell.selectedBackgroundView = CellSelectionImageView
        cell.tintColor = tag.category?.textColor
        cell.data = tag
        
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        super.tableView(tableView, didSelectRowAt: indexPath)
        let tag = tags[indexPath.row]
        self.delegate?.didUseSimilarTag(tag)
        self.hide()
    }
}
