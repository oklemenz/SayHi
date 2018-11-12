//
//  UserData.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 13.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import SwiftyJSON
import FirebaseAnalytics

let DefaultProfileName = "Standard".localized

let NameMaxLength = 30
let StatusMaxLength = 30
let BaseYear = 1900

class UserData {
    
    static let instance = UserData()
    
    var _initialized = false
    var initialized: Bool {
        set {
            _initialized = newValue
            requestInitialize = false
            if _initialized {
                initStandardProfile()
            }
        }
        get {
            return _initialized
        }
    }
    
    var _installationUUID: String = ""
    var installationUUID: String {
        set {
            _installationUUID = newValue
            FirebaseAnalytics.Analytics.setUserProperty(_installationUUID, forName: "installation")
        }
        get { return _installationUUID }
    }
    
    var _langCode: String = PrimaryLangCode
    var langCode: String {
        set {
            _langCode = newValue
            FirebaseAnalytics.Analytics.setUserProperty(_langCode, forName: "language")
        }
        get { return _langCode }
    }
    
    var _firstName: String = ""
    var firstName: String {
        set {
            _firstName = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        get { return _firstName }
    }
    
    var _gender: Gender = .none
    var gender: Gender {
        set {
            _gender = newValue
            FirebaseAnalytics.Analytics.setUserProperty(_gender.rawValue, forName: "gender")
        }
        get { return _gender }
    }
    
    var _birthYear: Int = 0
    var birthYear: Int {
        set {
            _birthYear = newValue
            FirebaseAnalytics.Analytics.setUserProperty(String(_birthYear), forName: "birthYear")
            FirebaseAnalytics.Analytics.setUserProperty(String(age), forName: "age")
        }
        get { return _birthYear }
    }
    
    var age : Int {
        let year: Int = Calendar.current.dateComponents([.year], from: Date()).year!
        if birthYear >= BaseYear && birthYear <= year {
            return year - birthYear
        }
        return 0
    }
    
    var _status: String = ""
    var status: String {
        set {
            _status = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        get { return _status }
    }
    
    var _matchMode: MatchMode = .open
    var matchMode: MatchMode {
        set {
            _matchMode = newValue
            FirebaseAnalytics.Analytics.setUserProperty(matchMode.externalDescription, forName: "matchMode")
            FirebaseAnalytics.Analytics.setUserProperty(String(_matchMode.rawValue), forName: "matchCode")
        }
        get { return _matchMode }
    }

    var _matchHandshake: Bool = false
    var matchHandshake: Bool {
        set {
            _matchHandshake = newValue
            FirebaseAnalytics.Analytics.setUserProperty("\(_matchHandshake)", forName: "matchHandshake")
        }
        get { return _matchHandshake }
    }
    
    
    var _touchID: Bool = false
    var touchID: Bool {
        set {
            _touchID = newValue
            if initialized {
                SecureStore.instance.setUseTouchID(_touchID)
            }
        }
        get { return _touchID }
    }
    
    var matchVibrate: Bool = Platform.isDevice
    var matchPlayGreeting: Bool = true
    var passcodeTimeout: PasscodeTimeout = .min5
    var inviteFriendSentCount: Int = 0
    var currentProfileId:Int = 0

    var greetingVoice: Data?
    var profiles: [Profile] = []
    var history: [Match] = []
    
    var scoreMatchCount: Int = 0
    var bothPosScore: Int = 0
    var bothNegScore: Int = 0
    var onlyPosScore: Int = 0
    var onlyNegScore: Int = 0
    var matchScore: Int {
        return bothPosScore + bothNegScore + onlyPosScore + onlyNegScore
    }
    
    var highscoreLocal: String = ""
    var localScore: Int = 0
    var shareScore: Int {
        return matchScore - localScore
    }
    var scoreLocalCount: Int = 0
    var scoreShareCount: Int {
        return scoreMatchCount - scoreLocalCount
    }
    
    var newItemsHash: [String:Int] = [:]
    var ownTags: [String:Tag] = [:]
    
    var qrHelpFirstShown: Bool = false
    
    var standardSettings: [String: Any] = [:]
    
    var requestInitialize: Bool = false
    
    init() {
        setup()
        NotificationCenter.default.addObserver(self, selector: #selector(handleFetch), name: UserDataFetchNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleStore), name: UserDataChangedNotification, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    func setup() {
        self.installationUUID = UUID().uuidString
        
        self.langCode = PrimaryLangCode
        if Locale.current.languageCode != nil && BundleLangCodes.contains(Locale.current.languageCode!) {
            self.langCode = Locale.current.languageCode!
        }
        self.gender = .none
        self.birthYear = 0
        self.matchMode = .open
        self.matchHandshake = false
        
        if !SecureStore.appInitialized() {
            SecureStore.instance.setUseTouchID(touchID)
        }
    }

    func initialize(error: (() -> ())? = nil, completion : (() -> ())? = nil) {
        var userInfo : [String:Any] = [:]
        if let completion = completion {
            userInfo["completion"] = completion
        }
        if let error = error {
            userInfo["error"] = error
        }
        NotificationCenter.default.post(name: UserDataFetchNotification, object: nil, userInfo: userInfo)
    }
    
    func touch(error: (() -> ())? = nil, completion: (() -> ())? = nil) {
        var userInfo : [String:Any] = [:]
        if let completion = completion {
            userInfo["completion"] = completion
        }
        if let error = error {
            userInfo["error"] = error
        }
        NotificationCenter.default.post(name: UserDataChangedNotification, object: nil, userInfo: userInfo)
    }
    
    func addProfile(name: String) -> Int {
        let index = 0
        _ = createProfile(name: name)
        self.touch()
        return index
    }
    
    func createProfile(name: String) -> Profile {
        let profile = Profile(id: self.nextProfileId, name: name)
        Analytics.instance.logAddProfile(profile: profile)
        self.profiles.insert(profile, at: 0)
        return profile
    }
    
    func renameProfile(_ profile: Profile, name: String) -> Int {
        profile.name = name
        Analytics.instance.logChangeProfile(profile: profile)
        self.touch()
        return self.profiles.index(where: { $0 === profile }) ?? -1
    }
    
    func removeProfile(_ profile: Profile) -> Int? {
        for (index, aProfile) in profiles.enumerated() {
            if aProfile.id == profile.id {
                if currentProfileId == profile.id {
                    currentProfileId = 0
                }
                profiles.remove(at: index)
                Analytics.instance.logRemoveProfile(profile: profile)
                self.touch()
                return index
            }
        }
        return nil
    }
    
    func copyProfile(_ profile: Profile, name: String) -> Int {
        let index = 0
        let profileCopy = Profile.fromJSON(json: profile.toJSON())
        profileCopy.id = self.nextProfileId
        profileCopy.name = name
        profileCopy.date = Date()
        Analytics.instance.logAddProfile(profile: profileCopy)
        self.profiles.insert(profileCopy, at: index)
        self.touch()
        return index
    }
    
    func changeMatchingMode(_ profile: Profile, matchMode: MatchMode?) {
        profile.matchMode = matchMode
        Analytics.instance.logChangeProfile(profile: profile)
        self.touch()
    }
    
    func changeRelationType(_ profile: Profile, relationType: RelationType) {
        profile.relationType = relationType
        Analytics.instance.logChangeProfile(profile: profile)
        self.touch()
    }
    
    func setCurrentProfile(_ profile: Profile) {
        currentProfileId = profile.id
    }
    
    func increaseInviteFriendSentCount() {
        self.inviteFriendSentCount += 1
        self.touch()
    }
    
    func sortProfiles() {
        self.profiles.sort { (profile1, profile2) -> Bool in
            return profile1.date > profile2.date
        }
    }
    
    func initStandardProfile() {
        if self.profiles.count == 0 {
            let profile = Profile(id: self.nextProfileId, name: DefaultProfileName)
            self.profiles.append(profile)
            self.currentProfileId = profile.id
        }
    }
    
    var currentProfile : Profile? {
        for profile in profiles {
            if profile.id == currentProfileId {
                return profile
            }
        }
        return nil
    }
    
    func getProfile(_ profileId: Int) -> Profile? {
        for profile in profiles {
            if profile.id == profileId {
                return profile
            }
        }
        return nil
    }
    
    var nextProfileId : Int {
        var maxId = 0
        for profile in profiles {
            maxId = max(maxId, profile.id)
        }
        return maxId + 1
    }
    
    func addOwnTag(_ tag: Tag) {
        tag.space = SecureStore.spaceRefName
        ownTags[tag.key] = tag
    }
    
    func addMatch(_ match: Match) {
        if installationExistsInDay(match.installationUUID) {
            match.counted = false
        } else {
            self.scoreMatchCount += 1
            self.bothPosScore += match.bothPosScore
            self.bothNegScore += match.bothNegScore
            self.onlyPosScore += match.onlyPosScore
            self.onlyNegScore += match.onlyNegScore
        }
        self.history.append(match)
        self.touch()
    }
    
    func installationExistsInDay(_ installationUUID: String) -> Bool {
        for match in history  {
            if match.installationUUID == installationUUID &&
                abs(match.date.timeIntervalSinceNow) <= 60 * 60 * 24 { // > 1 Day
                return true
            }
        }
        return false
    }
    
    func removeMatch(_ match: Match) {
        if let index = history.index(where: { (entry) -> Bool in
            return entry == match
        }) {
            history.remove(at: index)
        }
        self.touch()
    }
    
    func clearHistory() {
        history.removeAll()
        self.touch()
    }
    
    func addNewItemHash(_ hash: String) {
        if newItemsHash[hash] == nil {
            newItemsHash[hash] = 1
        } else {
            newItemsHash[hash] = newItemsHash[hash]! + 1
        }
    }
    
    func hasNewItemHash(_ hash: String) -> Int {
        return newItemsHash[hash] ?? 0
    }
    
    func spaceSwitched(_ space: String) {
        if space == StandardSpace {
            if let language = standardSettings["language"] as? String {
                self.langCode = language
            }
            if let matchCode = standardSettings["matchCode"] as? Int,
                let matchMode = MatchMode(rawValue: matchCode) {
                self.matchMode = matchMode
            }
            if let matchHandshake = standardSettings["matchHandshake"] as? Bool {
                self.matchHandshake = matchHandshake
            }
        } else {
            standardSettings["language"] = self.langCode
            standardSettings["matchCode"] = self.matchMode.rawValue
            standardSettings["matchHandshake"] = self.matchHandshake
        }
        UserData.instance.touch()
    }
    
    @objc func handleStore(sender: NSNotification) {
        let context = sender.userInfo as? [String:Any]
        let error = context?["error"] as? () -> ()
        let completion = context?["completion"] as? () -> ()
        store(errorOccurred: error, completion: completion)
    }
    
    @objc func handleFetch(sender: NSNotification) {
        let context = sender.userInfo as? [String:Any]
        let error = context?["error"] as? () -> ()
        let completion = context?["completion"] as? () -> ()
        fetch(errorOccurred: error, completion: completion)
    }
    
    func store(errorOccurred: (() -> ())? = nil, completion: (() -> ())? = nil) {
        DispatchQueue.global(qos: DispatchQoS.QoSClass.default).async {
            if !self.initialized {
                if let completion = completion {
                    DispatchQueue.main.async {
                        completion()
                    }
                }
                return
            }
            do {
                let userDataString = self.toJSONString()
                try SecureStore.instance.store(userDataString)
                if !SecureStore.appInitialized() {
                    Analytics.instance.logFirstStart()
                    SecureStore.setAppInitialized()
                }
                
                DispatchQueue.main.async {
                    if let completion = completion {
                        completion()
                    }
                    NotificationCenter.default.post(name: UserDataStoredNotification, object: nil)
                }
            } catch let error {
                print(error)
                if let errorOccurred = errorOccurred {
                    DispatchQueue.main.async {
                        errorOccurred()
                    }
                }
            }
        }
    }
    
    func fetch(errorOccurred: (() -> ())? = nil, completion : (() -> ())? = nil) {
        DispatchQueue.global(qos: DispatchQoS.QoSClass.default).async {
            if self.initialized {
                if let completion = completion {
                    DispatchQueue.main.async {
                        completion()
                    }
                }
                return
            }
            do {
                if SecureStore.appInitialized() {
                    if let userDataString = try SecureStore.instance.load() {
                        if let userData = userDataString.data(using: String.Encoding.utf8, allowLossyConversion: false) {
                            let userDataJSON = JSON(data: userData)
                            let raw = userDataJSON.rawValue as! [String: Any]
                            if let installationUUID = raw["i"] as? String {
                                if !installationUUID.isEmpty {
                                    self.installationUUID = installationUUID
                                }
                            }
                            self.langCode = raw["l"] as? String ?? PrimaryLangCode
                            self.firstName = raw["n"] as? String ?? ""
                            self.gender = Gender(rawValue: raw["g"] as? String ?? Gender.none.rawValue) ?? .none
                            self.birthYear = raw["y"] as? Int ?? BaseYear
                            self.status = raw["s"] as? String ?? ""
                            self.matchMode = MatchMode(rawValue: raw["m"] as? Int ?? MatchMode.open.rawValue) ?? .open
                            self.matchVibrate = raw["mv"] as? Bool ?? true
                            self.matchPlayGreeting = raw["mp"] as? Bool ?? true
                            self.matchHandshake = raw["mh"] as? Bool ?? false
                            let greetingVoiceString = raw["gv"] as? String ?? ""
                            if !greetingVoiceString.isEmpty {
                                self.greetingVoice = Data(base64Encoded: greetingVoiceString)
                            }
                            self.touchID = raw["t"] as? Bool ?? true
                            self.passcodeTimeout = PasscodeTimeout(rawValue: raw["pt"] as? Int ?? PasscodeTimeout.min5.rawValue) ?? .min5
                            self.inviteFriendSentCount = raw["f"] as? Int ?? 0
                            self.currentProfileId = raw["pi"] as? Int ?? 0
                            self.newItemsHash = raw["nh"] as? [String:Int] ?? [:]
                            self.ownTags = [:]
                            for (_, tagJSON) in userDataJSON["ot"].enumerated() {
                                let tag = Tag.fromJSON(json: tagJSON.1)
                                self.ownTags[tag.key] = tag
                            }
                            self.profiles = []
                            for (_, profileJSON) in userDataJSON["p"].enumerated() {
                                self.profiles.append(Profile.fromJSON(json: profileJSON.1))
                            }
                            self.history = []
                            for (_, historyJSON) in userDataJSON["h"].enumerated() {
                                self.history.append(Match.fromJSON(json: historyJSON.1))
                            }
                            self.scoreMatchCount = raw["smc"] as? Int ?? 0
                            self.bothPosScore = raw["sbp"] as? Int ?? 0
                            self.bothNegScore = raw["sbn"] as? Int ?? 0
                            self.onlyPosScore = raw["sop"] as? Int ?? 0
                            self.onlyNegScore = raw["son"] as? Int ?? 0
                            self.highscoreLocal = raw["hl"] as? String ?? ""
                            self.localScore = raw["sl"] as? Int ?? 0
                            self.scoreLocalCount = raw["slc"] as? Int ?? 0
                            self.qrHelpFirstShown = raw["qhs"] as? Bool ?? false
                            self.standardSettings = raw["ss"] as? [String:Any] ?? [:]
                            
                            self.initialized = true
                        }
                    } else {
                        self.recover()
                    }
                }

                self.sortProfiles()
                
                DispatchQueue.main.async {
                    if let completion = completion {
                        completion()
                    }
                    NotificationCenter.default.post(name: UserDataFetchedNotification, object: nil)
                }
            } catch let error {
                print(error)
                if let errorOccurred = errorOccurred {
                    DispatchQueue.main.async {
                        errorOccurred()
                    }
                }
            }
        }
    }
    
    func recover() {
        clear()
        setup()
        initialized = true
    }
    
    func clear() {
        SecureStore.instance.close()
        initialized = false
        
        self.installationUUID = ""
        self.langCode = PrimaryLangCode
        self.firstName = ""
        self.gender = Gender.none
        self.birthYear = 0
        self.status = ""
        self.matchMode = MatchMode.open
        self.matchVibrate = Platform.isDevice
        self.matchPlayGreeting = true
        self.matchHandshake = false
        self.greetingVoice = nil
        self.touchID = false
        self.passcodeTimeout = PasscodeTimeout.min5
        self.inviteFriendSentCount = 0
        self.currentProfileId = 0
        self.newItemsHash = [:]
        self.scoreMatchCount = 0
        self.bothPosScore = 0
        self.bothNegScore = 0
        self.onlyPosScore = 0
        self.onlyNegScore = 0
        self.highscoreLocal = ""
        self.localScore = 0
        self.scoreLocalCount = 0
        self.qrHelpFirstShown = false
        self.standardSettings = [:]
        self.ownTags = [:]
        self.profiles = []
        self.history = []
        
        NotificationCenter.default.post(name: UserDataClearedNotification, object: nil)
    }
    
    func toJSON() -> JSON {
        var ownTagsJSON : [JSON] = []
        for (_, entry) in ownTags {
            ownTagsJSON.append(entry.toJSON())
        }
        var profilesJSON : [JSON] = []
        for profile in profiles {
            profilesJSON.append(profile.toJSON())
        }
        var historyJSON : [JSON] = []
        for entry in history {
            historyJSON.append(entry.toJSON())
        }
        var raw: [String:Any] = [:]
        raw["i"] = installationUUID
        raw["l"] = langCode
        raw["n"] = firstName
        raw["g"] = gender.rawValue
        raw["y"] = birthYear
        raw["s"] = status
        raw["m"] = matchMode.rawValue
        raw["mv"] = matchVibrate
        raw["mp"] = matchPlayGreeting
        raw["mh"] = matchHandshake
        raw["gv"] = greetingVoice?.base64EncodedString() ?? ""
        raw["t"] = touchID
        raw["pt"] = passcodeTimeout.rawValue
        raw["f"] = inviteFriendSentCount
        raw["pi"] = currentProfileId
        raw["nh"] = newItemsHash
        raw["smc"] = scoreMatchCount
        raw["sbp"] = bothPosScore
        raw["sbn"] = bothNegScore
        raw["sop"] = onlyPosScore
        raw["son"] = onlyNegScore
        raw["hl"] = highscoreLocal
        raw["sl"] = localScore
        raw["slc"] = scoreLocalCount
        raw["qhs"] = qrHelpFirstShown
        raw["ss"] = standardSettings
        var json = JSON(raw)
        json["ot"] = JSON(ownTagsJSON)
        json["p"] = JSON(profilesJSON)
        json["h"] = JSON(historyJSON)
        return json
    }
    
    func toJSONString() -> String {
        return toJSON().rawString(String.Encoding.utf8, options: JSONSerialization.WritingOptions(rawValue: 0))!
    }
}
