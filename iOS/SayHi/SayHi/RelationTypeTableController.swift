//
//  RelationTypeTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 05.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

protocol RelationTypeTableControllerDelegate: class {
    func didSelectRelationType(_ relationType: RelationType, at indexPath: IndexPath)
}

class RelationTypeTableController : ListViewController {
    
    weak var delegate: RelationTypeTableControllerDelegate?
    var relationType: RelationType = RelationType.none
    var contextIndexPath : IndexPath!
    
    @IBAction func donePressed(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return RelationType.list.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "relationCell", for: indexPath)
        cell.selectionStyle = .default
        cell.selectedBackgroundView = CellSelectionImageView
        
        let relationType = RelationType.list[indexPath.row]
        cell.textLabel?.text = relationType.rawValue.codeLocalized
        
        cell.accessoryType = .none
        if self.relationType == relationType {
            cell.accessoryView = UIImageView(image: UIImage(named: "checkmark")?.colored(AccentColor))
        } else {
            cell.accessoryView = nil
        }
        
        return cell
    }
 
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        super.tableView(tableView, didSelectRowAt: indexPath)
        
        let relationType = RelationType.list[indexPath.row]
        self.relationType = relationType
        self.tableView.reloadData()
        self.dismiss(animated: true) {
            self.delegate?.didSelectRelationType(relationType, at: self.contextIndexPath)
        }
    }
}
