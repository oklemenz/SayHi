//
//  HighscoreTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 21.04.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class HighscoreTableController: ListViewController {
    
    var scores: [[String:Any]] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let text = NSMutableAttributedString(
            string: "Highscore".localized + "\n",
            attributes: [NSAttributedString.Key.foregroundColor: AccentColor])
        text.append(NSMutableAttributedString(
            string: SecureStore.space,
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

        self.activityIndicator = true        
        enableRefresh(true)
        refresh()
    }

    @objc override func refreshTriggered(_ sender: Any) {
        refresh(false)
    }
    
    func refresh(_ activityIndicator: Bool = true) {
        if activityIndicator {
            self.startActivityIndicator()
        }
        self.scores.removeAll()
        self.tableView.reloadData()
        
        _ = DataService.instance.fetchHighscore().then { (scores: [[String: Any]]) -> Void in
            self.scores = scores
            self.refreshControl?.endRefreshing()
            self.tableView.reloadData()
            if activityIndicator {
                self.stopActivityIndicator()
            }
        }
    }
    
    @IBAction func refreshPressed(_ sender: Any) {
        refresh()
    }
    
    @IBAction func donePressed(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    // MARK: TableView
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return scores.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "highscore", for: indexPath)
        cell.selectionStyle = .default
        
        let score = scores[indexPath.row]
        cell.selectedBackgroundView = CellSelectionImageView
        cell.textLabel?.text = "\(indexPath.row + 1). \(score["alias"] ?? "")"
        let scoreValue = score["value"] as? Int ?? 0
        let scoreCount = score["count"] as? Int ?? 0
        cell.detailTextLabel?.text = "\(String(format: scoreValue == 1 ? "%i pt.".localized : "%i pts.".localized, scoreValue)) (\(scoreCount))"
        cell.detailTextLabel?.textColor = AccentColor
        
        return cell
    }
}
