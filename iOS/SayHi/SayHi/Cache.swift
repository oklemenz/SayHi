//
//  Cache.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 17.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import SQLite

class Cache {
    
    static let instance = Cache()
    
    var db : Connection!
    let dbTag = Table("tag")
    let dbCategory = Table("category")
    let dbKey = Expression<String>("key")
    let dbData = Expression<String>("data")
    
    var tags: [String:Tag] = [:]
    var categories: [String:Category] = [:]
    
    var cachedTagKeys: Set<String> = []
    var cachedCategoryKeys: Set<String> = []
    
    init() {
        do {
            let documentsUrl = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let dbPath = documentsUrl.appendingPathComponent("cache.sqlite3")
            db = try Connection(dbPath.absoluteString)
        
            try db.run(dbTag.create(ifNotExists: true) { t in
                t.column(dbKey, primaryKey: true)
                t.column(dbData)
            })
            
            try db.run(dbCategory.create(ifNotExists: true) { t in
                t.column(dbKey, primaryKey: true)
                t.column(dbData)
            })
        } catch let error {
            print(error)
        }
    }
    
    func lookupTag(key: String) -> Tag? {
        do {
            for tag in try db.prepare(dbTag.filter(dbKey == key)) {
                return Tag.fromJSONString(jsonString: tag[dbData])
            }
        } catch let error {
            print(error)
        }
        return nil
    }
    
    func cacheTag(_ tag: Tag) {
        if cachedTagKeys.contains(tag.key) {
            return
        }
        cachedTagKeys.insert(tag.key)
        do {
            let upsert = self.dbTag.insert(or: .replace, self.dbKey <- tag.key, self.dbData <- tag.toJSONString())
            _ = try self.db.run(upsert)
        } catch let error {
            print(error)
        }
    }
    
    func cacheTagAsync(_ tag: Tag) {
        if cachedTagKeys.contains(tag.key) {
            return
        }
        weak var cacheTag = tag
        let dispatchTime = DispatchTime.now() + 0.1
        DispatchQueue.global(qos: DispatchQoS.QoSClass.background).asyncAfter(deadline: dispatchTime) {
            if let tag = cacheTag {
                self.cacheTag(tag)
            }
        }
    }
    
    func cacheTagsAsync(_ tags: [Tag]) {
        let dispatchTime = DispatchTime.now() + 0.5
        DispatchQueue.global(qos: DispatchQoS.QoSClass.background).asyncAfter(deadline: dispatchTime) {
            for tag in tags {
                self.cacheTag(tag)
            }
        }
    }
    
    func lookupCategory(key: String) -> Category? {
        do {
            for category in try db.prepare(dbCategory.filter(dbKey == key)) {
                return Category.fromJSONString(jsonString: category[dbData])
            }
        } catch let error {
            print(error)
        }
        return nil
    }
    
    func cacheCategory(_ category: Category) {
        if cachedCategoryKeys.contains(category.key) {
            return
        }
        cachedCategoryKeys.insert(category.key)
        do {
            let upsert = self.dbCategory.insert(or: .replace, self.dbKey <- category.key, self.dbData <- category.toJSONString())
            _ = try self.db.run(upsert)
        } catch let error {
            print(error)
        }
    }
    
    func cacheCategoryAsync(_ category: Category) {
        if cachedCategoryKeys.contains(category.key) {
            return
        }
        weak var cacheCategory = category
        let dispatchTime = DispatchTime.now() + 0.1
        DispatchQueue.global(qos: DispatchQoS.QoSClass.background).asyncAfter(deadline: dispatchTime) {
            if let category = cacheCategory {
                self.cacheCategory(category)
            }
        }
    }
    
    func cacheCategoriesAsync(_ categories: [Category]) {
        let dispatchTime = DispatchTime.now() + 0.5
        DispatchQueue.global(qos: DispatchQoS.QoSClass.background).asyncAfter(deadline: dispatchTime) {
            for category in categories {
                self.cacheCategory(category)
            }
        }
    }
}
