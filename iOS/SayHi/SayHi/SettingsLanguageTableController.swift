//
//  SettingsLanguageTableController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 01.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

protocol SettingsLanguageTableControllerDelegate: class {
    func didSelectLanguage(code: String)
}

class SettingsLanguageTableController: ListViewController {
    
    var favoriteLanguages : [[String:String]] = []
    var preferredLanguages : [[String:String]] = []
    var allLanguages : [[String:String]] = []
    
    var index : [String] = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"]
    var sections : [String] = []
    var entries : [String:[[String:String]]] = [:]
    
    var langMode = 0
    var selectedCode: String?
    weak var delegate: SettingsLanguageTableControllerDelegate?
    
    var isPresentedModal : Bool = false
    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    @IBAction func langTypeChanged(_ sender: UISegmentedControl) {
        langMode = sender.selectedSegmentIndex
        refresh()
        self.tableView.reloadData()
    }
    
    @objc func done(_ sender: Any) {
        if isPresentedModal {
            self.dismiss(animated: true, completion: nil)
        }
    }
   
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.sectionBackgroundClear = false
        self.tableView.sectionIndexColor = AccentColor
            
        favoriteLanguages = []
        for favoriteLanguage in Settings.instance.favoriteLanguages {
            favoriteLanguages.append(["code": favoriteLanguage, "name": Locale.current.localizedString(forIdentifier: favoriteLanguage) ?? ""])
        }
        favoriteLanguages.sort { (lang1, lang2) -> Bool in
            return lang1["name"]! < lang2["name"]!
        }
        
        preferredLanguages = []
        for preferredLanguage in ["en", "fr", "es", "zh", "ar", "pt", "ru", "de", "nl", "af", "hi", "bn", "ms", "id", "sw", "fa", "tr", "it", "ta", "jpn"] {
            preferredLanguages.append(["code": preferredLanguage, "name": Locale.current.localizedString(forIdentifier: preferredLanguage) ?? ""])
        }
        preferredLanguages.sort { (lang1, lang2) -> Bool in
            return lang1["name"]! < lang2["name"]!
        }
        
        for code in Locale.isoLanguageCodes {
            if let name = Locale.current.localizedString(forIdentifier: code) {
                allLanguages.append(["code": code, "name": name])
            }
        }
        allLanguages.sort { (lang1, lang2) -> Bool in
            return lang1["name"]! < lang2["name"]!
        }
        
        refresh()
    }
    
    func refresh() {
        var languages : [[String:String]] = []
        if langMode == 0 {
            languages = favoriteLanguages
        } else if langMode == 1 {
            languages = preferredLanguages
        } else if langMode == 2 {
            languages = allLanguages
        }
        
        sections.removeAll()
        entries.removeAll()
        for language in languages {
            let char = "\(language["name"]!.first!)".uppercased()
            if entries[char] == nil {
                entries[char] = []
                sections.append(char)
            }
            entries[char]?.append(language)
        }
        
        sections.sort()
    }
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        if langMode == 2 {
            return sections.count
        }
        return 1
    }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if langMode == 2 {
            return sections[section]
        }
        return nil
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if langMode == 0 {
            return favoriteLanguages.count
        } else if langMode == 1 {
            return preferredLanguages.count
        } else if langMode == 2 {
            return entries[sections[section]]!.count
        }
        return 0
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "languageCell", for: indexPath)

        var language : [String:String] = [:]
        if langMode == 0 {
            language = favoriteLanguages[indexPath.row]
        } else if langMode == 1 {
            language = preferredLanguages[indexPath.row]
        } else if langMode == 2 {
            language = entries[sections[indexPath.section]]![indexPath.row]
        }
        
        cell.selectionStyle = .default

        cell.accessoryType = .none
        if language["code"] == selectedCode {
            cell.accessoryView = UIImageView(image: UIImage(named: "checkmark")?.colored(AccentColor))
        } else {
            cell.accessoryView = nil
        }
        
        cell.selectedBackgroundView = CellSelectionImageView
        
        cell.textLabel?.text = language["name"]!
        
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        super.tableView(tableView, didSelectRowAt: indexPath)
        
        var language : [String:String] = [:]
        if langMode == 0 {
            language = favoriteLanguages[indexPath.row]
        } else if langMode == 1 {
            language = preferredLanguages[indexPath.row]
        } else if langMode == 2 {
            language = entries[sections[indexPath.section]]![indexPath.row]
        }
        
        selectedCode = language["code"]!
        self.delegate?.didSelectLanguage(code: selectedCode!)
        if isPresentedModal {
            self.dismiss(animated: true, completion: nil)
        } else {
            _ = self.navigationController?.popViewController(animated: true)
        }
    }
    
    override func sectionIndexTitles(for tableView: UITableView) -> [String]? {
        if langMode == 2 {
            return index
        }
        return []
    }
    
    override func tableView(_ tableView: UITableView, sectionForSectionIndexTitle title: String, at index: Int) -> Int {
        if let i = sections.index(of: title) {
            self.scrollDrag = true
            self.tableView.scrollToRow(at: IndexPath(row: 0, section: i), at: .top, animated: true)
        }
        return -1
    }
}
