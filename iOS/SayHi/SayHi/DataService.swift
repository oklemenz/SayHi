//
//  DataService.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 13.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import FirebaseDatabase
import SwiftyJSON
import PromiseKit

class DataService {

    enum DataServiceError: Error {
        case networkError
    }
    
    static let instance = DataService()
    
    var ref: DatabaseReference?
    var spaceRef: DatabaseReference?
    var settingsRef: DatabaseReference?
    var dataRef: DatabaseReference?
    var analyticsRef: DatabaseReference?
    var iconsRef: DatabaseReference?
    var messagesRef: DatabaseReference?
    var messageMatchRef: DatabaseReference?
    var matchesRef: DatabaseReference?
    var matchRef: DatabaseReference?
    var scoresRef: DatabaseReference?
    
    var favoriteCategories: [Category] = []
    var favoriteCategoriesLoaded: Bool = false
    
    var isSetup: Bool = false
    
    init() {
        NotificationCenter.default.addObserver(self, selector: #selector(loginSuccessful), name: LoginNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(spaceSwitched), name: SpaceSwitchedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(fetchFavoriteCategories), name: IconsFetchedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(fetchFavoriteCategories), name: UserDataFetchedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(fetchFavoriteCategories), name: UserDataLangChangedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(fetchFavoriteCategories), name: SetupEndNotification, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    func setup(_ spaceSwitch: Bool = false) {
        ref = Database.database().reference()
        spaceRef = ref?.child(SecureStore.spaceRefName)
        if let settingsRef = settingsRef {
            settingsRef.removeAllObservers()
        }
        settingsRef = spaceRef?.child("settings")
        dataRef = spaceRef?.child("data")
        analyticsRef = spaceRef?.child("analytics")
        iconsRef = spaceRef?.child("icons")
        messagesRef = spaceRef?.child("messages")
        matchesRef = spaceRef?.child("matches")
        scoresRef = spaceRef?.child("scores")
        isSetup = true
        NotificationCenter.default.post(name: DataServiceSetupNotification, object: nil)
        fetchSettings(spaceSwitch)
    }
    
    @objc func loginSuccessful() {
        setup()
    }
    
    @objc func spaceSwitched() {
        setup(true)
        fetchFavoriteCategories()
    }
    
    @objc func fetchFavoriteCategories() {
        favoriteCategoriesLoaded = false
        if dataRef != nil {
            var query = CategoryQuery()
            query.favorite = true
            _ = fetchCategories(query).then { (categories) -> () in
                self.favoriteCategories = categories
                self.favoriteCategoriesLoaded = true
                NotificationCenter.default.post(name: FavoriteCategoriesFetchedNotification, object: nil)
            }
        }
    }
    
    // MARK: Settings
    func fetchSettings(_ spaceSwitch: Bool = false) {
        if let settingsRef = settingsRef {
            settingsRef.removeAllObservers()
            settingsRef.observe(.value, with: { (snapshot) in
                if snapshot.exists() {
                    let settingsData = snapshot.value as! [String: Any]
                    Settings.instance.update(settingsData, spaceSwitch)
                    NotificationCenter.default.post(name: SettingsFetchedNotification, object: nil)
                }
            })
        }
    }
    
    // MARK: Icons
    func fetchIcons(date: Date?) -> Promise<[String:String]> {
        var iconsQuery: DatabaseQuery? = iconsRef
        if let date = date {
            iconsQuery = iconsQuery?.queryOrdered(byChild: "date").queryStarting(atValue: Int(date.timeIntervalSince1970 * 1000))
        }
        return Promise { (fulfill, reject) in
            var icons : [String:String] = [:]

            if let iconsQuery = iconsQuery {
                iconsQuery.observeSingleEvent(of: .value, with: { (snapshot) in
                    if snapshot.exists() {
                        for (_, snapshot) in snapshot.children.enumerated() {
                            let snapshot = snapshot as! DataSnapshot
                            let iconData = snapshot.value as! [String: Any]
                            if let data = iconData["data"] as? String {
                                icons[snapshot.key] = data
                            }
                        }
                    }
                    fulfill(icons)
                })
            } else {
                fulfill(icons)
            }
        }
    }
    
    // MARK: Active Tag/Category
    func parseCategorySnapshot(_ snapshot: DataSnapshot, langCode: String, suppressIgnore: Bool = false) -> Category? {
        let categoryData = snapshot.value as! [String: Any]
        if suppressIgnore || !(categoryData["ignore"] as? Bool ?? false) {
            let category = Category(key: snapshot.key,
                                    langCode: langCode,
                                    name: categoryData["name"] as! String,
                                    color: categoryData["color"] as! String,
                                    icon: categoryData["icon"] as! String,
                                    order: categoryData["order"] as! Int,
                                    primaryLangKey : categoryData["primaryLangKey"] as! String,
                                    refKey: categoryData["refKey"] as! String?,
                                    refPrimaryLangKey: categoryData["refPrimaryLangKey"] as! String?)
            category.favorite = categoryData["favorite"] as? Bool ?? false
            return category
        }
        return nil
    }
    
    func fetchCategories(_ query: CategoryQuery? = nil) -> Promise<[Category]> {
        let langCode = query != nil && !query!.langCode.isEmpty ? query!.langCode : UserData.instance.langCode
        let searchText = query?.searchText.searchNormalized(langCode: langCode) ?? ""
        
        let categoryRef : DatabaseReference? = dataRef?.child("\(langCode)/active/categories")
        var categoryQuery: DatabaseQuery? = categoryRef

        if let query = query {
            if query.favorite {
                categoryQuery = categoryRef?.queryOrdered(byChild: "favorite").queryEqual(toValue: true)
            } else if query.search {
                categoryQuery = categoryRef?.queryOrdered(byChild: "search")
                if !searchText.isEmpty {
                    categoryQuery = categoryQuery?.queryStarting(atValue: searchText)
                }
            } else if !query.name.isEmpty {
                categoryQuery = categoryRef?.queryOrdered(byChild: "name").queryEqual(toValue: query.name)
            } else if !query.primaryLangKey.isEmpty {
                categoryQuery = categoryRef?.queryOrdered(byChild: "primaryLangKey").queryEqual(toValue: query.primaryLangKey)
            }
        }
    
        return Promise { (fulfill, reject) in
            var categories : [Category] = []
            
            if let categoryQuery = categoryQuery {
                categoryQuery.queryLimited(toFirst: Settings.instance.queryLimit).observeSingleEvent(of: .value, with: { (snapshot) in
                    if snapshot.exists() {
                        for (_, snapshot) in snapshot.children.enumerated() {
                            if let category = self.parseCategorySnapshot(snapshot as! DataSnapshot, langCode: langCode) {
                                if query == nil || (searchText.isEmpty || category.search.hasPrefix(searchText)) {
                                    categories.append(category)
                                }
                            }
                        }
                        if query != nil && query!.favorite {
                            categories = categories.filter({
                                return $0.order > 0
                            })
                            categories.sort(by: {
                                return $0.order < $1.order
                            })
                        } else {
                            categories.sort(by: {
                                return $0.name < $1.name
                            })
                        }
                        Cache.instance.cacheCategoriesAsync(categories)
                    }
                    fulfill(categories)
                })
            } else {
                fulfill(categories)
            }
        }
    }
    
    func getCategory(key: String, langCode: String = UserData.instance.langCode) -> Promise<Category?> {
        let categoryRef : DatabaseReference? = dataRef?.child("\(langCode)/active/categories")
        return Promise { (fulfill, reject) in
            if let categoryRef = categoryRef {
                if !key.isEmpty {
                    if let category = Cache.instance.lookupCategory(key: key) {
                        fulfill(category)
                    } else {
                        categoryRef.child(key).observeSingleEvent(of: .value, with: { (snapshot) in
                            guard snapshot.exists() else {
                                return fulfill(nil)
                            }
                            if let category = self.parseCategorySnapshot(snapshot, langCode: langCode, suppressIgnore: true) {
                                Cache.instance.cacheCategoryAsync(category)
                                fulfill(category)
                            }
                            fulfill(nil)
                        })
                    }
                } else {
                    fulfill(nil)
                }
            } else {
                fulfill(nil)
            }
        }
    }
    
    func getCategories(keys: [String], langCode: String = UserData.instance.langCode) -> Promise<[Category?]> {
        return when(fulfilled: keys.map({ key in
            return getCategory(key: key, langCode: langCode)
        }))
    }
    
    func fetchSimilarCategories(name: String, langCode: String = UserData.instance.langCode) -> Promise<[Category]> {
        let searchText = name.searchNormalized(langCode: langCode)
        let categoryActiveRef: DatabaseReference? = dataRef?.child("\(langCode)/active/categories")
        let categoryActiveQuery = categoryActiveRef?.queryOrdered(byChild: "search").queryStarting(atValue: searchText)
        let categoryStageRef: DatabaseReference? = dataRef?.child("\(langCode)/stage/categories")
        let categoryStageQuery = categoryStageRef?.queryOrdered(byChild: "search").queryStarting(atValue: searchText)
        
        return when(fulfilled: [
            Promise<[Category]> { (fulfill, reject) in
                var categories: [Category] = []
                if let categoryActiveQuery = categoryActiveQuery {
                    categoryActiveQuery.queryLimited(toFirst: Settings.instance.queryLimit).observeSingleEvent(of: .value, with: { (snapshot) in
                        if snapshot.exists() {
                            for (_, snapshot) in snapshot.children.enumerated() {
                                if let category = self.parseCategorySnapshot(snapshot as! DataSnapshot, langCode: langCode) {
                                    if searchText.isEmpty || category.search.hasPrefix(searchText) {
                                        categories.append(category)
                                    }
                                }
                            }
                            Cache.instance.cacheCategoriesAsync(categories)
                        }
                        fulfill(categories)
                    })
                } else {
                    fulfill(categories)
                }
            },
            Promise<[Category]> { (fulfill, reject) in
                var categories: [Category] = []
                if let categoryStageQuery = categoryStageQuery {
                    categoryStageQuery.queryLimited(toFirst: Settings.instance.queryLimit).observeSingleEvent(of: .value, with: { (snapshot) in
                        if snapshot.exists() {
                            for (_, snapshot) in snapshot.children.enumerated() {
                                if let stageCategory = self.parseStageCategorySnapshot(snapshot as! DataSnapshot, langCode: langCode) {
                                    let category = stageCategory.category
                                    if searchText.isEmpty || category.search.hasPrefix(searchText) {
                                        categories.append(category)
                                    }
                                }
                            }
                        }
                        fulfill(categories)
                    })
                } else {
                    fulfill(categories)
                }
            },
        ]).then { (result) -> Promise<[Category]> in
            var categories = result.reduce([], +)
            categories.sort(by: {
                return $0.name < $1.name
            })
            return Promise(value: categories)
        }
    }
    
    func completeCategory(_ category: Category) -> Promise<Category> {
        return completeCategories([category]).then { categories -> Promise<Category> in
            return Promise(value: categories[0])
        }
    }
    
    func completeCategories(_ categories: [Category]) -> Promise<[Category]> {
        return when(fulfilled: categories.map({ category -> Promise<Void> in
            var promises = [Promise<Void>]()
            if category._primaryLangCategory == nil && !category.primaryLangKey.isEmpty {
                promises.append(self.getCategory(key: category.primaryLangKey, langCode: PrimaryLangCode).then{ (primaryLangCategory) -> Promise<Void> in
                    if category._primaryLangCategory == nil {
                        category._primaryLangCategory = primaryLangCategory
                    }
                    return Promise<Void>(value: Void())
                })
                promises.append(self.getStageCategory(key: category.primaryLangKey, langCode: PrimaryLangCode).then{ (primaryLangStageCategory) -> Promise<Void> in
                    if category._primaryLangCategory == nil {
                        category._primaryLangCategory = primaryLangStageCategory?.category
                    }
                    return Promise<Void>(value: Void())
                })
            }
            return when(fulfilled: promises)
        })).then { (result) -> Promise<[Category]> in
            return Promise(value: categories)
        }
    }
    
    func parseTagSnapshot(_ snapshot: DataSnapshot, langCode: String, suppressIgnore: Bool = false) -> Tag? {
        let tagData = snapshot.value as! [String: Any]
        if suppressIgnore || !(tagData["ignore"] as? Bool ?? false) {
            let tag = Tag(key: snapshot.key,
                          langCode: langCode,
                          name: tagData["name"] as! String,
                          categoryKey: tagData["categoryKey"] as! String,
                          primaryLangKey : tagData["primaryLangKey"] as! String,
                          refKey: tagData["refKey"] as! String?,
                          refPrimaryLangKey: tagData["refPrimaryLangKey"] as! String?)
            tag.favorite = tagData["favorite"] as? Bool ?? false
            tag.space = SecureStore.spaceRefName
            return tag
        }
        return nil
    }
    
    func fetchTags(_ query: TagQuery) -> Promise<[Tag]> {
        let langCode = !query.langCode.isEmpty ? query.langCode : UserData.instance.langCode
        let searchText = query.searchText.searchNormalized(langCode: langCode)
        
        let tagRef : DatabaseReference? = dataRef?.child("\(langCode)/active/tags")
        var tagsQuery: DatabaseQuery?
        
        if query.favorite {
            tagsQuery = tagRef?.queryOrdered(byChild: "favorite").queryEqual(toValue: true)
        } else if query.search {
            tagsQuery = tagRef?.queryOrdered(byChild: "search")
            if !searchText.isEmpty  {
                tagsQuery = tagsQuery?.queryStarting(atValue: searchText)
            }
        } else if !query.name.isEmpty {
            tagsQuery = tagRef?.queryOrdered(byChild: "name").queryEqual(toValue: query.name)
        } else if !query.categoryKey.isEmpty {
            tagsQuery = tagRef?.queryOrdered(byChild: "categoryKey").queryEqual(toValue: query.categoryKey)
        } else if query.categoryStaged {
            tagsQuery = tagRef?.queryOrdered(byChild: "categoryStaged").queryEqual(toValue: true)
        }
        
        return Promise { (fulfill, reject) in
            var tags : [Tag] = []
            
            if let tagsQuery = tagsQuery {
                tagsQuery.queryLimited(toFirst: Settings.instance.queryLimit).observeSingleEvent(of: .value, with: { (snapshot) in
                    if snapshot.exists() {
                        for (_, snapshot) in snapshot.children.enumerated() {
                            if let tag = self.parseTagSnapshot(snapshot as! DataSnapshot, langCode: langCode) {
                                if !query.name.isEmpty {
                                    if !query.categoryKey.isEmpty {
                                        if query.categoryKey == tag.categoryKey {
                                            tags.append(tag)
                                        }
                                    } else {
                                        tags.append(tag)
                                    }
                                } else if query.searchText.isEmpty || tag.search.hasPrefix(searchText) {
                                    tags.append(tag)
                                }
                            }
                        }
                        tags.sort(by: {
                            return $0.name < $1.name
                        })
                        Cache.instance.cacheTagsAsync(tags)
                    }
                    fulfill(tags)
                })
            } else {
                fulfill(tags)
            }
        }
    }
    
    func hasTags(langCode: String = UserData.instance.langCode) -> Promise<Bool> {
        let tagRef: DatabaseReference? = dataRef?.child("\(langCode)/active/tags")
        if tagRef == nil {
            return Promise<Bool>(error: DataServiceError.networkError)
        }
        let tagQuery = tagRef!.queryOrderedByKey().queryLimited(toFirst: 1)
        return Promise<Bool> { (fulfill, reject) in
            tagQuery.observeSingleEvent(of: .value, with: { (snapshot) in
                fulfill(snapshot.exists())
            })
        }
    }
    
    func getTag(key: String, langCode: String = UserData.instance.langCode) -> Promise<Tag?> {
        let tagRef: DatabaseReference? = dataRef?.child("\(langCode)/active/tags")
        return Promise { (fulfill, reject) in
            if let tagRef = tagRef {
                if !key.isEmpty {
                    if let tag = Cache.instance.lookupTag(key: key) {
                        fulfill(tag)
                    } else {
                        tagRef.child(key).observeSingleEvent(of: .value, with: { (snapshot) in
                            guard snapshot.exists() else {
                                return fulfill(nil)
                            }
                            if let tag = self.parseTagSnapshot(snapshot, langCode: langCode, suppressIgnore: true) {
                                Cache.instance.cacheTagAsync(tag)
                                fulfill(tag)
                            }
                            fulfill(nil)
                        })
                    }
                } else {
                    fulfill(nil)
                }
            } else {
                fulfill(nil)
            }
        }
    }
    
    func getTags(keys: [String], langCode: String = UserData.instance.langCode) -> Promise<[Tag?]> {
        return when(fulfilled: keys.map({ key in
            return getTag(key: key, langCode: langCode)
        }))
    }
    
    func fetchSimilarTags(name: String, langCode: String = UserData.instance.langCode) -> Promise<[Tag]> {
        let searchText = name.searchNormalized(langCode: langCode)
        let tagActiveRef: DatabaseReference? = dataRef?.child("\(langCode)/active/tags")
        let tagActiveQuery = tagActiveRef?.queryOrdered(byChild: "search").queryStarting(atValue: searchText)
        let tagStageRef: DatabaseReference? = dataRef?.child("\(langCode)/stage/tags")
        let tagStageQuery = tagStageRef?.queryOrdered(byChild: "search").queryStarting(atValue: searchText)
        
        return when(fulfilled: [
            Promise<[Tag]> { (fulfill, reject) in
                var tags: [Tag] = []
                if let tagActiveQuery = tagActiveQuery {
                    tagActiveQuery.queryLimited(toFirst: Settings.instance.queryLimit).observeSingleEvent(of: .value, with: { (snapshot) in
                        if snapshot.exists() {
                            for (_, snapshot) in snapshot.children.enumerated() {
                                if let tag = self.parseTagSnapshot(snapshot as! DataSnapshot, langCode: langCode) {
                                    if searchText.isEmpty || tag.search.hasPrefix(searchText) {
                                        tags.append(tag)
                                    }
                                }
                            }
                            Cache.instance.cacheTagsAsync(tags)
                        }
                        fulfill(tags)
                    })
                } else {
                    fulfill(tags)
                }
            },
            Promise<[Tag]> { (fulfill, reject) in
                var tags: [Tag] = []
                if let tagStageQuery = tagStageQuery {
                    tagStageQuery.queryLimited(toFirst: Settings.instance.queryLimit).observeSingleEvent(of: .value, with: { (snapshot) in
                        if snapshot.exists() {
                            for (_, snapshot) in snapshot.children.enumerated() {
                                if let stageTag = self.parseStageTagSnapshot(snapshot as! DataSnapshot, langCode: langCode) {
                                    let tag = stageTag.tag
                                    if searchText.isEmpty || tag.search.hasPrefix(searchText) {
                                        tags.append(tag)
                                    }
                                }
                            }
                        }
                        fulfill(tags)
                    })
                } else {
                    fulfill(tags)
                }
            },
        ]).then { (result) -> Promise<[Tag]> in
            var tags = result.reduce([], +)
            tags.sort(by: {
                return $0.name < $1.name
            })
            return Promise(value: tags)
        }
    }
    
    func completeTag(_ tag: Tag) -> Promise<Tag> {
        return completeTags([tag]).then { tags -> Promise<Tag> in
            return Promise(value: tags[0])
        }
    }
    
    func completeTags(_ tags: [Tag]) -> Promise<[Tag]> {
        return when(fulfilled: tags.map({ tag -> Promise<Void> in
            var promises = [Promise<Void>]()
            if tag._category == nil  && !tag.categoryKey.isEmpty {
                promises.append(self.getCategory(key: tag.categoryKey, langCode: tag.langCode).then{ (category) -> Promise<Void> in
                    if tag._category == nil {
                        tag._category = category
                    }
                    return Promise<Void>(value: Void())
                })
                promises.append(self.getStageCategory(key: tag.categoryKey, langCode: tag.langCode).then{ (stageCategory) -> Promise<Void> in
                    if tag._category == nil {
                        tag._category = stageCategory?.category
                    }
                    return Promise<Void>(value: Void())
                })
            }
            if tag._primaryLangTag == nil && !tag.primaryLangKey.isEmpty {
                promises.append(self.getTag(key: tag.primaryLangKey, langCode: PrimaryLangCode).then{ (primaryLangTag) -> Promise<Void> in
                    if tag._primaryLangTag == nil {
                        tag._primaryLangTag = primaryLangTag
                    }
                    return Promise<Void>(value: Void())
                })
                promises.append(self.getStageTag(key: tag.primaryLangKey, langCode: PrimaryLangCode).then{ (primaryLangStageTag) -> Promise<Void> in
                    if tag._primaryLangTag == nil {
                        tag._primaryLangTag = primaryLangStageTag?.tag
                    }
                    return Promise<Void>(value: Void())
                })
            }
            return when(fulfilled: promises)
        })).then { (result) -> Promise<[Tag]> in
            return when(fulfilled: tags.map({ tag -> Promise<Void> in
                var promises = [Promise<Void>]()
                if let category = tag.category {
                    if category._primaryLangCategory == nil && !category.primaryLangKey.isEmpty {
                        promises.append(self.getCategory(key: category.primaryLangKey, langCode: PrimaryLangCode).then{ (primaryLangCategory) -> Promise<Void> in
                            if category._primaryLangCategory == nil {
                                category._primaryLangCategory = primaryLangCategory
                            }
                            return Promise<Void>(value: Void())
                        })
                        promises.append(self.getStageCategory(key: category.primaryLangKey, langCode: PrimaryLangCode).then{ (primaryLangCategory) -> Promise<Void> in
                            if category._primaryLangCategory == nil {
                                category._primaryLangCategory = primaryLangCategory?.category
                            }
                            return Promise<Void>(value: Void())
                        })
                    }
                }
                return when(fulfilled: promises)
            })).then { (result) -> Promise<[Tag]> in
                return Promise(value: tags)
            }
        }
    }
    
    // MARK: Stage Tag/Category
    func parseStageSnapshot(_ snapshot: DataSnapshot, suppressIgnore: Bool = false) -> (key: String, name: String)? {
        let tagData = snapshot.value as! [String: Any]
        if suppressIgnore || !(tagData["ignore"] as? Bool ?? false) {
            return (key: snapshot.key, name: tagData["name"] as! String)
        }
        return nil
    }
    
    func parseStageCategorySnapshot(_ snapshot: DataSnapshot, langCode: String, suppressIgnore: Bool = false) -> StageCategory? {
        let categoryData = snapshot.value as! [String: Any]
        if suppressIgnore || !(categoryData["ignore"] as? Bool ?? false) {
            return StageCategory(key: snapshot.key,
                                 langCode: langCode,
                                 name: categoryData["name"] as! String,
                                 primaryLangKey: categoryData["primaryLangKey"] as! String,
                                 counter: categoryData["counter"] as! Int)
        }
        return nil
    }
    
    func getStageCategory(key: String, langCode: String = UserData.instance.langCode) -> Promise<StageCategory?> {
        let categoryRef : DatabaseReference? = dataRef?.child("\(langCode)/stage/categories")
        return Promise { (fulfill, reject) in
            if let categoryRef = categoryRef {
                if !key.isEmpty {
                    categoryRef.child(key).observeSingleEvent(of: .value, with: { (snapshot) in
                        guard snapshot.exists() else {
                            return fulfill(nil)
                        }
                        let stageCategory = self.parseStageCategorySnapshot(snapshot, langCode: langCode, suppressIgnore: true)
                        fulfill(stageCategory)
                    })
                } else {
                    fulfill(nil)
                }
            } else {
                fulfill(nil)
            }
        }
    }
    
    func createCategory(_ newCategory : NewCategory) -> Promise<StageCategory?> {
        let hash = "#C:\(newCategory.name)#P:\(newCategory.primaryLangCategory?.key ?? "")"
        let categoryRef: DatabaseReference? = dataRef?.child("\(newCategory.langCode)/stage/categories")
        if categoryRef == nil {
            return Promise<StageCategory?>(error: DataServiceError.networkError)
        }
        let categoryQuery: DatabaseQuery = categoryRef!.queryOrdered(byChild: "hash").queryEqual(toValue: hash)
        return Promise<(key: String, name: String)?> { (fulfill, reject) in
            categoryQuery.observeSingleEvent(of: .value, with: { (snapshot) in
                if snapshot.exists() {
                    for (_, snapshot) in snapshot.children.enumerated() {
                        let stage = self.parseStageSnapshot(snapshot as! DataSnapshot)
                        fulfill(stage)
                        return
                    }
                }
                fulfill(nil)
            })
        }.then { stage in
            if stage == nil {
                return Promise { (fulfill, reject) in
                    let category = categoryRef!.childByAutoId()
                    newCategory.key = category.key
                    var data : [String:Any] = [:]
                    data["name"] = newCategory.name
                    data["language"] = newCategory.langCode
                    data["search"] = newCategory.name.searchNormalized(langCode: newCategory.langCode)
                    data["icon"] = newCategory.primaryLangCategory?.name.cleaned.lowercased() ?? newCategory.name.cleaned.lowercased()
                    data["primaryLangKey"] = newCategory.primaryLangCategory?.key ?? ""
                    data["hash"] = hash
                    data["counter"] = 1
                    data["createdAt"] = ServerValue.timestamp()
                    data["changedAt"] = ServerValue.timestamp()
                    category.setValue(data, withCompletionBlock: { (error, ref) in
                        UserData.instance.addNewItemHash(hash)
                        UserData.instance.touch()
                        DispatchQueue.main.async() {
                            if let error = error {
                                reject(error)
                            } else {
                                fulfill(())
                            }
                        }
                    })
                }
            } else {
                newCategory.key = stage!.key
                return Promise { (fulfill, reject) in
                    categoryRef!.child(newCategory.key).runTransactionBlock({ (currentData) -> TransactionResult in
                        if var categoryData = currentData.value as? [String: Any] {
                            categoryData["counter"] = categoryData["counter"] as! Int + (UserData.instance.hasNewItemHash(hash) == 0 ? 1 : 0)
                            categoryData["changedAt"] = ServerValue.timestamp()
                            currentData.value = categoryData
                        }
                        return TransactionResult.success(withValue: currentData)
                    }, andCompletionBlock: { (error, commited, snapshot) in
                        UserData.instance.addNewItemHash(hash)
                        UserData.instance.touch()
                        DispatchQueue.main.async() {
                            if let error = error {
                                reject(error)
                            } else {
                                fulfill(())
                            }
                        }
                    })
                }
            }
        }.then {
            return self.getStageCategory(key: newCategory.key, langCode: newCategory.langCode)
        }.then { category -> Promise<StageCategory?> in
            if let category = category {
                Analytics.instance.logNewCategory(category: category)
            }
            return Promise<StageCategory?>(value: category)
        }
    }
    
    func parseStageTagSnapshot(_ snapshot: DataSnapshot, langCode: String, suppressIgnore: Bool = false) -> StageTag? {
        let tagData = snapshot.value as! [String: Any]
        if suppressIgnore || !(tagData["ignore"] as? Bool ?? false) {
            return StageTag(key: snapshot.key,
                            langCode: langCode,
                            name: tagData["name"] as! String,
                            categoryKey: tagData["categoryKey"] as! String,
                            categoryName: tagData["categoryName"] as! String,
                            primaryLangKey: tagData["primaryLangKey"] as! String,
                            counter: tagData["counter"] as! Int)
        }
        return nil
    }
    
    func getStageTag(key: String, langCode: String = UserData.instance.langCode) -> Promise<StageTag?> {
        let tagRef : DatabaseReference? = dataRef?.child("\(langCode)/stage/tags")
        return Promise { (fulfill, reject) in
            if let tagRef = tagRef {
                if !key.isEmpty {
                    tagRef.child(key).observeSingleEvent(of: .value, with: { (snapshot) in
                        guard snapshot.exists() else {
                            return fulfill(nil)
                        }
                        let stageTag = self.parseStageTagSnapshot(snapshot, langCode: langCode, suppressIgnore: true)
                        fulfill(stageTag)
                    })
                } else {
                    fulfill(nil)
                }
            } else {
                fulfill(nil)
            }
        }
    }
    
    func createTag(_ newTag: NewTag) -> Promise<StageTag?> {
        let hash = "#T:\(newTag.name)#C:\(newTag.category!.key)#P:\(newTag.primaryLangTag?.key ?? "")"
        let tagRef: DatabaseReference? = dataRef?.child("\(newTag.langCode)/stage/tags")
        if tagRef == nil {
            return Promise<StageTag?>(error: DataServiceError.networkError)
        }
        let tagQuery: DatabaseQuery = tagRef!.queryOrdered(byChild: "hash").queryEqual(toValue: hash)
        return Promise<(key: String, name: String)?> { (fulfill, reject) in
            tagQuery.observeSingleEvent(of: .value, with: { (snapshot) in
                if snapshot.exists() {
                    for (_, snapshot) in snapshot.children.enumerated() {
                        let stage = self.parseStageSnapshot(snapshot as! DataSnapshot)
                        fulfill(stage)
                        return
                    }
                }
                fulfill(nil)
            })
            }.then { stage in
                if stage == nil {
                    return Promise { (fulfill, reject) in
                        let tag = tagRef!.childByAutoId()
                        newTag.key = tag.key
                        var data : [String:Any] = [:]
                        data["name"] = newTag.name
                        data["language"] = newTag.langCode
                        data["search"] = newTag.name.searchNormalized(langCode: newTag.langCode)
                        data["categoryKey"] = newTag.category!.key
                        data["categoryName"] = newTag.category!.name
                        data["primaryLangKey"] = newTag.primaryLangTag?.key ?? ""
                        data["hash"] = hash
                        data["counter"] = 1
                        data["createdAt"] = ServerValue.timestamp()
                        data["changedAt"] = ServerValue.timestamp()
                        tag.setValue(data, withCompletionBlock: { (error, ref) in
                            UserData.instance.addNewItemHash(hash)
                            UserData.instance.touch()
                            DispatchQueue.main.async() {
                                if let error = error {
                                    reject(error)
                                } else {
                                    fulfill(())
                                }
                            }
                        })
                    }
                } else {
                    newTag.key = stage!.key
                    return Promise { (fulfill, reject) in
                        tagRef!.child(newTag.key).runTransactionBlock({ (currentData) -> TransactionResult in
                            if var tagData = currentData.value as? [String: Any] {
                                tagData["counter"] = tagData["counter"] as! Int + (UserData.instance.hasNewItemHash(hash) == 0 ? 1 : 0)
                                tagData["changedAt"] = ServerValue.timestamp()
                                currentData.value = tagData
                            }
                            return TransactionResult.success(withValue: currentData)
                        }, andCompletionBlock: { (error, commited, snapshot) in
                            UserData.instance.addNewItemHash(hash)
                            UserData.instance.touch()
                            DispatchQueue.main.async() {
                                if let error = error {
                                    reject(error)
                                } else {
                                    fulfill(())
                                }
                            }
                        })
                    }
                }
            }.then {
                return self.getStageTag(key: newTag.key, langCode: newTag.langCode)
            }.then { tag -> Promise<StageTag?> in
                if let tag = tag {
                    Analytics.instance.logNewTag(tag: tag)
                }
                return Promise<StageTag?>(value: tag)
        }
    }
    
    // MARK: Message
    func createMessage(_ content: String) -> Promise<String> {
        return Promise<String> { (fulfill, reject) in
            if let messagesRef = messagesRef {
                let messageRef = messagesRef.childByAutoId()
                let data: [String: Any] = ["content": content,
                                           "match": "",
                                           "date": ServerValue.timestamp()]
                messageRef.setValue(data, withCompletionBlock: { (error, ref) in
                    DispatchQueue.main.async() {
                        if let error = error {
                            reject(error)
                        } else {
                            fulfill(messageRef.key)
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }

    func observeMessageMatch(_ key: String, updated: ((String) -> ())?) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            stopObserveMessageMatch()
            if let messagesRef = messagesRef {
                messageMatchRef = messagesRef.child(key).child("match")
                messageMatchRef!.observe(.value, with: { (snapshot) in
                    guard snapshot.exists() else {
                        return fulfill(())
                    }
                    fulfill(())
                    if let updated = updated,
                        let value = snapshot.value as? String {
                        if !value.isEmpty {
                            updated(value)
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func stopObserveMessageMatch() {
        if let messageMatchRef = messageMatchRef {
            messageMatchRef.removeAllObservers()
            self.messageMatchRef = nil
        }
    }
    
    func updateMessageMatch(_ key: String, match: String) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            if let messagesRef = messagesRef {
                let messageMatchRef = messagesRef.child(key).child("match")
                messageMatchRef.setValue(match, withCompletionBlock: { (error, ref) in
                    DispatchQueue.main.async() {
                        if let error = error {
                            reject(error)
                        } else {
                            fulfill(())
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func messageContent(_ key: String) -> Promise<String?> {
        return Promise<String?> { (fulfill, reject) in
            if let messagesRef = messagesRef {
                let messageRef = messagesRef.child(key).child("content")
                messageRef.observeSingleEvent(of: .value, with: { (snapshot) in
                    guard snapshot.exists() else {
                        return fulfill(nil)
                    }
                    fulfill(snapshot.value as? String)
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func removeMessage(_ key: String) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            if let messagesRef = messagesRef {
                let messageRef = messagesRef.child(key)
                messageRef.removeValue(completionBlock: { (error, ref) in
                    DispatchQueue.main.async() {
                        if let error = error {
                            reject(error)
                        } else {
                            fulfill(())
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }

    // MARK: Match
    func createMatchPart(_ key: String, isFirst: Bool, session: String?) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            if let matchesRef = matchesRef {
                let matchRef = matchesRef.child(key)
                var data: [String: Any] = [
                    "date": ServerValue.timestamp()
                ]
                if isFirst {
                    data["first"] = ["active": true,
                                     "session": session ?? ""]
                } else {
                    data["second"] = ["active": true,
                                      "session": session ?? ""]
                }
                matchRef.updateChildValues(data, withCompletionBlock: { (error, ref) in
                    DispatchQueue.main.async() {
                        if let error = error {
                            reject(error)
                        } else {
                            fulfill(())
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func parseMatchStatusSnapshot(_ snapshot: DataSnapshot) -> (MatchStatus) {
        let data = snapshot.value as! [String: Any]
        let first = data["first"] as? [String: Any]
        let second = data["second"] as? [String: Any]
        return MatchStatus(active1: first?["active"] as? Bool,
                           session1: first?["session"] as? String,
                           active2: second?["active"] as? Bool,
                           session2: second?["session"] as? String)
    }
    
    func observeMatchStatus(_ key: String, updated: ((MatchStatus) -> ())?) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            stopObserveMatchStatus()
            if let matchesRef = matchesRef {
                matchRef = matchesRef.child(key)
                matchRef!.observe(.value, with: { (snapshot) in
                    guard snapshot.exists() else {
                        return fulfill(())
                    }
                    fulfill(())
                    if let updated = updated {
                        updated(self.parseMatchStatusSnapshot(snapshot))
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func stopObserveMatchStatus() {
        if let matchRef = matchRef {
            matchRef.removeAllObservers()
            self.matchRef = nil
        }
    }
    
    func setMatchPartInactive(_ key: String, isFirst: Bool) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            if let matchesRef = matchesRef {
                var matchRef: DatabaseReference!
                if isFirst {
                    matchRef = matchesRef.child(key).child("first/active")
                } else {
                    matchRef = matchesRef.child(key).child("second/active")
                }
                matchRef.setValue(false, withCompletionBlock: { (error, ref) in
                    DispatchQueue.main.async() {
                        if let error = error {
                            reject(error)
                        } else {
                            fulfill(())
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func removeInactiveMatch(_ key: String) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            if let matchesRef = matchesRef {
                let matchRef = matchesRef.child(key)
                matchRef.runTransactionBlock({ (currentData) -> TransactionResult in
                    if let data = currentData.value as? [String: Any] {
                        let first = data["first"] as? [String: Any]
                        let second = data["second"] as? [String: Any]
                        if (first == nil || first?["active"] as? Bool == false) &&
                            (second == nil || second?["active"] as? Bool == false) {
                            currentData.value = nil
                            return TransactionResult.success(withValue: currentData)
                        } else {
                            return TransactionResult.abort()
                        }
                    }
                    return TransactionResult.success(withValue: currentData)
                }, andCompletionBlock: { (error, commited, snapshot) in
                    DispatchQueue.main.async() {
                        if let error = error {
                            reject(error)
                        } else {
                            fulfill(())
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }

    // MARK: Space
    func fetchSpaceMeta(space: String) -> Promise<[String: Any]?> {
        return Promise<[String:Any]?> { (fulfill, reject) in
            if let spaceRef = ref?.child(space) {
                let spaceMetaRef = spaceRef.child("meta")
                spaceMetaRef.observeSingleEvent(of: .value, with: { (snapshot) in
                    guard snapshot.exists() else {
                        return fulfill(nil)
                    }
                    if let spaceMeta = snapshot.value as? [String: Any] {
                        if !(spaceMeta["ignore"] as? Bool ?? false) {
                            return fulfill(spaceMeta)
                        }
                    }
                    return fulfill(nil)
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func verifySpaceProtection(space: String, accessCode: String) -> Promise<Bool> {
        return Promise<Bool> { (fulfill, reject) in
            if let spaceRef = ref?.child(space) {
                let spaceProtectionRef = spaceRef.child("protection").child(accessCode)
                spaceProtectionRef.observeSingleEvent(of: .value, with: { (snapshot) in
                    guard snapshot.exists() else {
                        return fulfill(false)
                    }
                    fulfill(true)
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    // MARK: Scores
    func shareHighscore(alias: String, value: Int, count: Int) -> Promise<Void> {
        return Promise<Void> { (fulfill, reject) in
            if let scoresRef = scoresRef {
                let scoresRef = scoresRef.child(UserData.instance.installationUUID)
                let data: [String: Any] = ["alias": alias,
                                           "value": value,
                                           "count": count]
                scoresRef.setValue(data, withCompletionBlock: { (error, ref) in
                    DispatchQueue.main.async() {
                        if let error = error {
                            reject(error)
                        } else {
                            fulfill(())
                        }
                    }
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    func fetchHighscore() -> Promise<[[String: Any]]> {
        return Promise<[[String:Any]]> { (fulfill, reject) in
            if let scoresRef = scoresRef {
                let scoresQuery = scoresRef.queryOrdered(byChild: "value").queryLimited(toLast: 100)
                scoresQuery.observeSingleEvent(of: .value, with: { (snapshot) in
                    var scores = [[String: Any]]()
                    guard snapshot.exists() else {
                        return fulfill(scores)
                    }
                    for (_, snapshot) in snapshot.children.enumerated() {
                        let snapshot = snapshot as! DataSnapshot
                        if let score = snapshot.value as? [String: Any] {
                            scores.append(score)
                        }
                    }
                    scores.sort(by: {
                        return $0["value"] as! Int > $1["value"] as! Int
                    })
                    return fulfill(scores)
                })
            } else {
                reject(DataServiceError.networkError)
            }
        }
    }
    
    // MARK: Analytics
    func logEvent(_ event: String, parameters : [String:NSObject]) {
        if let analyticsRef = analyticsRef {
            let components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
            var data = parameters
            data["event"] = event as NSObject
            data["date"] = ServerValue.timestamp() as NSObject
            data["year"] = components.year! as NSObject
            data["month"] = components.month! as NSObject
            data["day"] = components.day! as NSObject
            data["cluster"] = "\(components.year!)/\(String(format: "%02d", components.month!))" as NSObject
            data["installation"] = UserData.instance.installationUUID as NSObject
            data["space"] = SecureStore.spaceRefName as NSObject
            data["language"] = UserData.instance.langCode as NSObject
            data["gender"] = UserData.instance.gender.rawValue as NSObject
            data["birthYear"] = UserData.instance.birthYear as NSObject
            data["age"] = UserData.instance.age as NSObject
            data["defaultMatchMode"] = UserData.instance.matchMode.externalDescription as NSObject
            data["defaultMatchCode"] = UserData.instance.matchMode.rawValue as NSObject
            data["device"] = "iOS" as NSObject
            data["deviceLanguage"] = (Locale.current.languageCode ?? "") as NSObject
            data["deviceLocale"] = Locale.current.identifier as NSObject
            analyticsRef.childByAutoId().setValue(data)
        }
    }
}
