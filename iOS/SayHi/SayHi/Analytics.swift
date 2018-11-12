//
//  Analytics.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 21.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import FirebaseAnalytics

func +<K, V> ( left: [K : V], right: [K : V]) -> [K : V] {
    var map = [K : V]()
    for (k, v) in left {
        map[k] = v
    }
    for (k, v) in right {
        map[k] = v
    }
    return map
}

class Analytics {
    
    static let instance = Analytics()

    func parameters(_ profile : Profile) -> [String:NSObject] {
        let matchMode = profile.matchMode != nil ? profile.matchMode!.externalDescription : ""
        let matchCode = profile.matchMode != nil ? profile.matchMode!.rawValue : -1
        return [
            "profileMatchMode" : matchMode as NSObject,
            "profileMatchCode" : matchCode as NSObject,
            "profileRelationType" : profile.relationType.rawValue as NSObject,
            "profilePosTagCount" : profile.posTags.count as NSObject,
            "profileNegTagCount" : profile.negTags.count as NSObject
        ]
    }
    
    func parameters(_ tag : Tag) -> [String:NSObject] {
        return [
            "tagKey" : tag.key as NSObject,
            "tagName" : tag.name as NSObject,
            "tagPrimaryLangKey" : tag.primaryLangKey as NSObject,
            "tagRefKey" : (tag.refKey ?? "") as NSObject,
            "tagRefPrimaryLangKey" : (tag.refPrimaryLangKey ?? "") as NSObject,
            "tagEffectiveKey" : tag.effectiveKey as NSObject,
            "categoryKey" : tag.categoryKey as NSObject,
            "categoryName" : (tag.category?.name ?? "") as NSObject,
            "categoryPrimaryLangKey" : (tag.category?.primaryLangKey ?? "") as NSObject,
            "categoryRefKey" : (tag.category?.refKey ?? "") as NSObject,
            "categoryRefPrimLangKey" : (tag.category?.refPrimaryLangKey ?? "") as NSObject
        ]
    }
    
    func parameters(_ category : Category) -> [String:NSObject] {
        return [
            "categoryKey" : category.key as NSObject,
            "categoryName" : category.name as NSObject,
            "categoryPrimaryLangKey" : category.primaryLangKey as NSObject,
            "categoryRefKey" : (category.refKey ?? "") as NSObject,
            "categoryRefPrimLangKey" : (category.refPrimaryLangKey ?? "") as NSObject,
            "categoryEffectiveKey" : category.effectiveKey as NSObject
        ]
    }
    
    func parameters(_ tag : StageTag) -> [String:NSObject] {
        return [
            "tagKey" : tag.key as NSObject,
            "tagName" : tag.name as NSObject,
            "categoryKey" : tag.categoryKey as NSObject,
            "categoryName" : tag.categoryName as NSObject,
            "counter" : tag.counter as NSObject
        ]
    }
    
    func parameters(_ category : StageCategory) -> [String:NSObject] {
        return [    
            "categoryKey" : category.key as NSObject,
            "categoryName" : category.name as NSObject,
            "counter" : category.counter as NSObject
        ]
    }
    
    func parameters(_ match : Match) -> [String:NSObject] {
        return [
            "matchDate" : match.date.iso as NSObject,
            "matchMode" : match.mode.description as NSObject,
            "matchCode" : match.mode.rawValue as NSObject,
            "matchHandshake" : match.handshake as NSObject,
            "profileRelationType" : match.relationType.rawValue as NSObject,
            "profilePosTagCount" : match.profilePosTagCount as NSObject,
            "profileNegTagCount" : match.profileNegTagCount as NSObject,
            "locationLongitude" : match.locationLongitude as NSObject,
            "locationLatitude" : match.locationLatitude as NSObject,
            "locationStreet" : match.locationStreet as NSObject,
            "locationCity" : match.locationCity as NSObject,
            "locationCountry" : match.locationCountry as NSObject,
            "messageLanguage" : match.langCode as NSObject,
            "messageGender" : match.gender.rawValue as NSObject,
            "messageBirthYear" : match.birthYear as NSObject,
            "messageAge" : match.age as NSObject,
            "messageInstallation" : match.installationUUID as NSObject,
            "messagePosTagCount" : match.messagePosTagCount as NSObject,
            "messageNegTagCount" : match.messageNegTagCount as NSObject,
            "matchLeftLeftTagKeys" : match.bothPosTagEffectiveKeys.joined(separator: ",") as NSObject,
            "matchRightRightTagKeys" : match.bothNegTagEffectiveKeys.joined(separator: ",") as NSObject,
            "matchLeftRightTagKeys" : match.onlyPosTagEffectiveKeys.joined(separator: ",") as NSObject,
            "matchRightLeftTagKeys" : match.onlyNegTagEffectiveKeys.joined(separator: ",") as NSObject
        ]
    }
    
    func logFirstStart()  {
        let params: [String:NSObject] = [:]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("first_start", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("first_start", parameters: params)
        }
    }
    
    func logAddProfile(profile : Profile)  {
        let params = parameters(profile) + [
            AnalyticsParameterValue : 1 as NSObject,
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("profile", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("profile", parameters: params)
        }
    }
    
    func logChangeProfile(profile : Profile)  {
        let params = parameters(profile) + [
            AnalyticsParameterValue : 0 as NSObject,
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("profile", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("profile", parameters: params)
        }
    }
    
    func logRemoveProfile(profile : Profile)  {
        let params = parameters(profile) + [
            AnalyticsParameterValue : -1 as NSObject,
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("profile", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("profile", parameters: params)
        }
    }
    
    func logPositive(tag : Tag, previousValue : Int) {
        let params = parameters(tag) + [
            AnalyticsParameterValue : 1 as NSObject,
            "previousValue": previousValue as NSObject
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("tag_assign", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("tag_assign", parameters: params)
        }
    }
    
    func logNegative(tag : Tag, previousValue : Int) {
        let params = parameters(tag) + [
            AnalyticsParameterValue : -1 as NSObject,
            "previousValue": previousValue as NSObject
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("tag_assign", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("tag_assign", parameters: params)
        }
    }
    
    func logNeutral(tag : Tag, previousValue : Int) {
        let params = parameters(tag) + [
            AnalyticsParameterValue : 0 as NSObject,
            "previousValue": previousValue as NSObject
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("tag_assign", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("tag_assign", parameters: params)
        }
    }
    
    func logMatch(match: Match, session: String, messageSession: String) {
        var params = parameters(match)
        params["matchSession"] = session as NSObject
        params["messageSession"] = messageSession as NSObject
        params["matchLeftLeftTagKeys"] = (params["matchLeftLeftTagKeys"] as! String).prefix(100) as NSObject
        params["matchRightRightTagKeys"] = (params["matchRightRightTagKeys"] as! String).prefix(100) as NSObject
        params["matchLeftRightTagKeys"] = (params["matchLeftRightTagKeys"] as! String).prefix(100) as NSObject
        params["matchRightLeftTagKeys"] = (params["matchRightLeftTagKeys"] as! String).prefix(100) as NSObject
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("match", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            let params = parameters(match)
            DataService.instance.logEvent("match", parameters: params)
        }
    }
    
    func logNewTag(tag : StageTag) {
        let params = parameters(tag)
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("tag_new", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("tag_new", parameters: params)
        }
    }
    
    func logNewCategory(category : StageCategory) {
        let params = parameters(category)
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("category_new", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("category_new", parameters: params)
        }
    }
    
    func logInviteFriend() {
        let params = [
            "count" : UserData.instance.inviteFriendSentCount as NSObject
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("invite_friend", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("invite_friend", parameters: params)
        }
    }
    
    func logRateApp() {
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("rate_app", parameters: [:])
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("rate_app", parameters: [:])
        }
    }
    
    func logSupportMail() {
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("support_feedback", parameters: [:])
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("support_feedback", parameters: [:])
        }
    }
    
    func logRecordVoice() {
        let params = [
            AnalyticsParameterValue : 1 as NSObject,
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("record_voice", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("record_voice", parameters: params)
        }
    }
    
    func logRerecordVoice() {
        let params = [
            AnalyticsParameterValue : 0 as NSObject,
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("record_voice", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("record_voice", parameters: params)
        }
    }
    
    func logRemoveRecordedVoice() {
        let params = [
            AnalyticsParameterValue : -1 as NSObject,
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("record_voice", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("record_voice", parameters: params)
        }
    }
    
    func logSpaceSwitched(_ space: String) {
        let params = [
            "space" : space as NSObject,
        ]
        if !Settings.instance.disableRecordAnalytics {
            FirebaseAnalytics.Analytics.logEvent("space_switch", parameters: params)
        }
        if !Settings.instance.disableRecordAnalyticsDB {
            DataService.instance.logEvent("space_switch", parameters: params)
        }
    }
}
