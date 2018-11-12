//
//  IconService.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 09.01.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import SQLite

let IconFetchDateField = "\(Namespace).IconFetchDate"

class IconService {
    
    static let instance = IconService()
    
    var db : Connection!
    let dbIcon = Table("icon")
    let dbKey = Expression<String>("key")
    let dbData = Expression<String>("data")
    
    var icons : [String:UIImage] = [:]
    
    init() {
        do {
            let documentsUrl = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let dbPath = documentsUrl.appendingPathComponent("icon.sqlite3")
            db = try Connection(dbPath.absoluteString)
            
            try db.run(dbIcon.create(ifNotExists: true) { t in
                t.column(dbKey, primaryKey: true)
                t.column(dbData)
            })
        } catch let error {
            print(error)
        }
        NotificationCenter.default.addObserver(self, selector: #selector(spaceSwitched), name: SpaceSwitchedNotification, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc func spaceSwitched() {
        clear()
        fetch()
    }
    
    func clear() {
        UserDefaults.standard.removeObject(forKey: IconFetchDateField)
        UserDefaults.standard.synchronize()
    }
    
    func reset() {
        clear()
        do {
            let delete = self.dbIcon.delete()
            _ = try self.db.run(delete)
        } catch let error {
            print(error)
        }
        fetch()
    }
    
    func fetch() {
        var iconFetchDate = UserDefaults.standard.string(forKey: IconFetchDateField)?.dateFromISO
        _ = DataService.instance.fetchIcons(date: iconFetchDate).then { (icons) -> () in
            if icons.count > 0 {
                for (key, data) in icons {
                    do {
                        let upsert = self.dbIcon.insert(or: .replace, self.dbKey <- key, self.dbData <- data)
                        _ = try self.db.run(upsert)
                    } catch let error {
                        print(error)
                    }
                }
                
                iconFetchDate = Date()
                iconFetchDate?.addTimeInterval(-2*60*60) // 2 hour adjustment, e.g. daylight saving time (DST)
                UserDefaults.standard.set(iconFetchDate?.iso, forKey: IconFetchDateField)
                UserDefaults.standard.synchronize()
                
                NotificationCenter.default.post(name: IconsFetchedNotification, object: nil)
            }
        }
    }
    
    func icon(_ name: String) -> UIImage? {
        if let image = UIImage(named: name) {
            return image
        }
        var name = name
        if UIScreen.main.isRetina {
            name = "\(name)@2x"
        }
        if icons[name] != nil {
            return icons[name]
        }
        do {
            for icon in try db.prepare(dbIcon.filter(dbKey == name)) {
                if let data = Data(base64Encoded: icon[dbData]) {
                    if var image = UIImage(data: data) {
                        image = UIImage(cgImage: image.cgImage!, scale: UIScreen.main.screenScale, orientation: .up)
                        icons[name] = image
                        return image
                    }
                }
            }
        } catch let error {
            print(error)
        }
        return nil
    }
}
