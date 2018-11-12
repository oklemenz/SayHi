//
//  HistoryTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class HistoryTableController : ListViewController {
    
    var profile : Profile!
    var days : [Date] = []
    var matchesByDay : [Date:[Match]] = [:]
    
    var titleLabel: UILabel!
    
    var deleteAllButton: UIBarButtonItem!

    @IBOutlet weak var helpMatchInformationLabel: UILabel!
    @IBOutlet weak var helpMatchInformationArrow: UIImageView!    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.sectionBackgroundClear = false
        self.navigationItem.rightBarButtonItems!.append(self.editButtonItem)
        self.deleteAllButton = UIBarButtonItem(image: UIImage(named: "delete"), style: .plain, target: self, action: #selector(clearAllPressed))
        
        self.titleLabel = UILabel(frame: CGRect(x:0, y:0, width:200, height:50))
        self.titleLabel.backgroundColor = UIColor.clear
        self.titleLabel.numberOfLines = 2
        self.titleLabel.font = UIFont.boldSystemFont(ofSize: 16.0)
        self.titleLabel.textAlignment = .center
        self.titleLabel.textColor = UIColor.white
        self.navigationItem.titleView = titleLabel
        self.updateLabels()
        
        helpMatchInformationLabel.text = "MatchInformation".termLocalized(Emoji.like, Emoji.dislike, Emoji.relationType, Emoji.matchMode)
        if helpMatchInformationLabel.text!.isEmpty {
            helpMatchInformationArrow.isHidden = true
        }
        
        refresh()
    }
    
    func refresh() {
        days.removeAll()
        matchesByDay.removeAll()
        
        for match in UserData.instance.history {
            let dayDate = match.date.dayDate
            if !days.contains(dayDate) {
                days.append(dayDate)
            }
            if matchesByDay[dayDate] == nil {
                matchesByDay[dayDate] = []
            }
            matchesByDay[dayDate]?.append(match)
        }
        
        days.sort(by: >)
        for entry in matchesByDay {
            matchesByDay[entry.key] = entry.value.sorted(by: {
                return $0.date > $1.date
            })
        }
        tableView.reloadData()
    }
    
    func updateLabels() {
        let text = NSMutableAttributedString(
            string: String(format: UserData.instance.scoreMatchCount == 1 ?
                "%i Match".localized : "%i Matches".localized, UserData.instance.scoreMatchCount),
            attributes: [NSAttributedStringKey.foregroundColor: AccentColor])
        text.append(NSMutableAttributedString(
            string: "\n" +
                String(format: UserData.instance.matchScore == 1 ? "%i Matching Point".localized : "%i Matching Points".localized, UserData.instance.matchScore),
            attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                         NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
        titleLabel.attributedText = text
    }
    
    @objc func clearAllPressed() {
        let alertController = UIAlertController(title: "Delete Matching History".localized, message: "DeleteAllMatches".aliasLocalized, preferredStyle: .alert)
        let deleteAction = UIAlertAction(title: "OK".localized, style: .destructive, handler: { (action : UIAlertAction) in
            UserData.instance.clearHistory()
            self.isEditing = false
            self.refresh()
        })
        alertController.addAction(deleteAction)
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: { (action : UIAlertAction) in
        })
        alertController.addAction(cancelAction)
        alertController.view?.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }

    override func setEditing(_ editing: Bool, animated: Bool) {
        super.setEditing(editing, animated: animated)
        if let rightBarButtonItems = self.navigationItem.rightBarButtonItems {
            if let index = rightBarButtonItems.index(of: deleteAllButton) {
                self.navigationItem.rightBarButtonItems?.remove(at: index)
            }
            if editing && UserData.instance.history.count > 0 {
                self.navigationItem.rightBarButtonItems?.insert(deleteAllButton, at: helpVisible ? 1 : 0)
            }
        }
    }
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return days.count
    }
    
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        let day = days[section]
        return longDateFormatter.string(from: day)
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let day = days[section]
        return matchesByDay[day]!.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "matchCell", for: indexPath)
        cell.selectionStyle = .default
        cell.accessoryType = .none
        cell.accessoryView = UIImageView(image: UIImage(named: "arrow_right"))
        cell.selectedBackgroundView = CellSelectionImageView
        
        let day = days[indexPath.section]
        let match = matchesByDay[day]![indexPath.row]

        if let imageView = cell.imageView {
            let color = match.counted ? QRCode.color : QRCode.inactiveColor
            let text = Crypto.hash("\(match.firstName)\(match.gender.rawValue)\(match.age)").prefixStr(16)
            imageView.image = QRCode.instance.generate(text: text, size: 22, scale: 1.0, color: color)
        }
        
        let text = NSMutableAttributedString(string: !match.firstName.isEmpty ? match.firstName : "?".localized)
        if match.gender != .none {
            text.append(NSMutableAttributedString(string: ", " +
                String(format: "\(match.gender.rawValue)_short".codeLocalized)))
        }
        if match.birthYear >= BaseYear {
            text.append(NSMutableAttributedString(string: ", " +
                String(format: "~%i y.".localized, match.age)))
        }
        text.append(NSMutableAttributedString(string: ", \(match.langCode.uppercased())"))
        text.append(NSMutableAttributedString(
            string: "  " +
                String(format: "LikeDislikeNum".aliasLocalized, Emoji.like, match.messagePosTagCount, Emoji.dislike, match.messageNegTagCount) +
                    "  ",
            attributes: [NSAttributedStringKey.foregroundColor: AccentColor,
                         NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
        
        var ptsAttributes: [NSAttributedStringKey:Any] = [
            NSAttributedStringKey.foregroundColor: AccentColor,
            NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]
        if !match.counted {
            ptsAttributes[NSAttributedStringKey.strikethroughColor] = AccentColor
            ptsAttributes[NSAttributedStringKey.baselineOffset] = 0
        }
        let ptsText = NSMutableAttributedString(
            string: String(format: match.score == 1 ? "(%i pt.)".localized : "(%i pts.)".localized, match.score),
            attributes: ptsAttributes)
        if !match.counted {
            ptsText.addAttributes([
                NSAttributedStringKey.strikethroughStyle: 1
            ], range: NSMakeRange(0, ptsText.length))
        }
        text.append(ptsText)
        cell.textLabel?.attributedText = text
        cell.textLabel?.lineBreakMode = .byTruncatingMiddle
        cell.textLabel?.alpha = match.counted ? 1.0 : 0.5
        
        let detailText = NSMutableAttributedString(
            string: match.profileName,
            attributes: [NSAttributedStringKey.foregroundColor: UIColor.black])
        if match.relationType != .none {
            detailText.append(NSMutableAttributedString(
                string: SeparatorString + Emoji.relationType +
                    match.relationType.rawValue.codeLocalized,
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black]))
        }
        detailText.append(NSMutableAttributedString(
            string: SeparatorString + Emoji.matchMode +
                match.mode.description.codeLocalized,
            attributes: [NSAttributedStringKey.foregroundColor: UIColor.black]))
        var separator = "  "
        if !match.locationCity.isEmpty {
            detailText.append(NSMutableAttributedString(
                string:  separator +
                match.locationCity,
                attributes: [NSAttributedStringKey.foregroundColor: AccentColor,
                             NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]
            ))
            separator = ", "
        }
        detailText.append(NSMutableAttributedString(
            string: separator +
                timeFormatter.string(from: match.date),
            attributes: [NSAttributedStringKey.foregroundColor: AccentColor,
                         NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]
        ))
        cell.detailTextLabel?.attributedText = detailText
        cell.detailTextLabel?.lineBreakMode = .byTruncatingMiddle
        cell.detailTextLabel?.alpha = match.counted ? 1.0 : 0.5
        
        return cell
    }
    
    func matchDelete(_ indexPath: IndexPath, completion : ((Bool) -> ())? = nil ) {
        let alertController = UIAlertController(title: "Match Deletion".localized, message: "DeleteMatch".aliasLocalized, preferredStyle: .alert)
        let deleteAction = UIAlertAction(title: "Delete".localized, style: .destructive, handler: { (action : UIAlertAction) in
            let day = self.days[indexPath.section]
            let match = self.matchesByDay[day]![indexPath.row]
            self.matchesByDay[day]!.remove(at: indexPath.row)
            if self.matchesByDay[day]!.isEmpty {
                self.matchesByDay.removeValue(forKey: day)
                self.days.remove(at: indexPath.section)
            }
            UserData.instance.removeMatch(match)
            
            self.tableView.beginUpdates()
            self.tableView.deleteRows(at: [indexPath], with: .automatic)
            if self.matchesByDay[day] == nil {
                self.tableView.deleteSections(IndexSet(integer: indexPath.section), with: .automatic)
            }
            self.tableView.endUpdates()
            self.updateLabels()
            if let completion = completion {
                completion(true)
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
    
    @available(iOS 11.0, *)
    override func tableView(_ tableView: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath: IndexPath) -> UISwipeActionsConfiguration? {
        var actions : [UIContextualAction] = []
        let deleteAction = UIContextualAction(style: .destructive, title: "Delete".localized, handler: { (action, view, completion) in
            self.matchDelete(indexPath, completion: completion)
        })
        actions.append(deleteAction)
        return UISwipeActionsConfiguration(actions: actions)
    }
    
    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        return true
    }
    
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCellEditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == UITableViewCellEditingStyle.delete {            
            self.matchDelete(indexPath)
        }
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "match",
            let matchViewController = segue.destination as? MatchViewController {
            let indexPath = self.tableView.indexPathForSelectedRow!
            let day = days[indexPath.section]
            let match = matchesByDay[day]![indexPath.row]
            matchViewController.match = match
            matchViewController.profile = profile
        }
    }
}
