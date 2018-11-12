//
//  QRContent.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 30.01.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation

let MatchMessageLength = 20 // 20: Firebase Key
let MatchSessionLength = 20 // 20: Session Key

let MatchMaxLength = MatchSessionLength + MatchMessageLength

struct QRContent {
    
    var message: String
    var session: String
    
    var description : String {
        let parts : [String] = [
            message.prefixStr(MatchMessageLength),
            session.prefixStr(MatchSessionLength)
        ]
        return parts.joined(separator: "")
    }
    
    static func generate(message: String, session: String) -> QRContent {
        return QRContent(message: message,
                         session: session)
    }
    
    static func parse(_ text: String) -> QRContent? {
        if text.count == MatchMaxLength {
            let message = text.substring(with: 0..<MatchMessageLength)
            let session = text.substring(with: MatchMessageLength..<MatchMaxLength)
            return QRContent(message: message,
                             session: session)
        } else if text.count == MatchMessageLength {
            return QRContent(message: text,
                             session: "")
        }
        return nil
    }
}
