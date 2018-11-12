//
//  Match.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation

struct Message {

    static let MatchPartCount = 13
    static let PartSeparatorChracter = "\0"
    static let TagSeparatorChracter = "\t"
    
    var langCode: String
    var firstName: String
    var gender: Gender
    var birthYear: Int
    var status: String
    var space: String
    var installationUUID: String
    var matchMode: MatchMode
    var matchHandshake: Bool
    var posTags : [String] = []
    var negTags : [String] = []
    
    var description : String {
        let parts : [String] = [
            UserData.instance.langCode,
            firstName.condensed,
            gender.rawValue,
            String(birthYear),
            status.condensed,
            space.condensed,
            installationUUID,
            String(matchMode.rawValue),
            String(matchHandshake ? 1 : 0),
            String(posTags.count),
            posTags.joined(separator: Message.TagSeparatorChracter),
            String(negTags.count),
            negTags.joined(separator: Message.TagSeparatorChracter)
        ]
        return parts.joined(separator: Message.PartSeparatorChracter)
    }
    
    static func generate(profile: Profile) -> Message {
        let userData = UserData.instance
        return Message(langCode: userData.langCode,
                       firstName: userData.firstName,
                       gender: userData.gender,
                       birthYear: userData.birthYear,
                       status: userData.status,
                       space: SecureStore.spaceRefName,
                       installationUUID: userData.installationUUID,
                       matchMode: profile.effectiveMatchMode,
                       matchHandshake: userData.matchHandshake,
                       posTags: profile.posTagEffectiveKeys,
                       negTags: profile.negTagEffectiveKeys)
    }
    
    static func parse(_ text: String) -> Message? {
        let parts = text.components(separatedBy: PartSeparatorChracter)
        if parts.count == MatchPartCount {
            let langCode = parts[0]
            let firstName = parts[1]
            let gender = Gender(rawValue: parts[2]) ?? .none
            let birthYear = Int(parts[3]) ?? BaseYear
            let status = parts[4]
            let space = parts[5]
            let installationUUID = parts[6]
            let matchMode = MatchMode(rawValue: Int(parts[7]) ?? MatchMode.open.rawValue) ?? .open
            let matchHandshake = Int(parts[8]) == 1
            let _ = Int(parts[9]) ?? 0
            let posTags = !parts[10].isEmpty ? parts[10].components(separatedBy: TagSeparatorChracter) : []
            let _ = Int(parts[11]) ?? 0
            let negTags = !parts[12].isEmpty ? parts[12].components(separatedBy: TagSeparatorChracter) : []
            
            return Message(langCode: langCode,
                           firstName: firstName,
                           gender: gender,
                           birthYear: birthYear,
                           status: status,
                           space: space,
                           installationUUID: installationUUID,
                           matchMode: matchMode,
                           matchHandshake: matchHandshake,
                           posTags: posTags,
                           negTags: negTags)
        }
        return nil
    }
}
