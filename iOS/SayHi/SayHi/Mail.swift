//
//  Mail.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 07.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit
import MessageUI

class Mail : NSObject, MFMailComposeViewControllerDelegate, MFMessageComposeViewControllerDelegate {

    let MailSupportTag = 1
    let MailInviteTag = 2
    
    static let instance = Mail()
    
    var presenter : UIViewController!
    var mailCompletion : ((MFMailComposeResult) -> ())?
    var messageCompletion : ((MessageComposeResult) -> ())?
    
    func sendSupportMail(presenter : UIViewController, completion : ((MFMailComposeResult) -> ())? = nil) {
        if MFMailComposeViewController.canSendMail() {
            self.presenter = presenter
            self.mailCompletion = completion
            
            let mail = MFMailComposeViewController()
            mail.view.tag = MailSupportTag
            mail.view.tintColor = AccentColor
            mail.mailComposeDelegate = self
            mail.setToRecipients(["ContactMail".aliasLocalized])
            mail.setSubject("ContactSubject".aliasLocalized)
            let messageBody = String(format: "ContactBody".aliasLocalized, UserData.instance.firstName)
            mail.setMessageBody(messageBody, isHTML: true)
            mail.navigationBar.titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.black]
            self.presenter.present(mail, animated: true)
        } else {
            alertDeviceNoMail()
        }
    }
    
    func sendInviteFriendsMail(presenter : UIViewController, completion : ((MFMailComposeResult) -> ())? = nil) {
        self.presenter = presenter
        if MFMailComposeViewController.canSendMail() {
            self.mailCompletion = completion
            
            let mail = MFMailComposeViewController()
            mail.view.tag = MailInviteTag
            mail.view.tintColor = AccentColor
            mail.mailComposeDelegate = self
            mail.setSubject("InviteSubject".aliasLocalized)
            let messageBody = String(format: "InviteBody".aliasLocalized,
                                     "AppStoreUrlIOS".aliasLocalized,
                                     "AppStoreUrlIOS".aliasLocalized,
                                     "AppStoreUrlAndroid".aliasLocalized,
                                     "AppStoreUrlAndroid".aliasLocalized,
                                     UserData.instance.firstName)
            mail.setMessageBody(messageBody, isHTML: true)
            mail.navigationBar.titleTextAttributes = [NSAttributedString.Key.foregroundColor: AccentColor]
            self.presenter.present(mail, animated: true)
        } else {
            self.alertDeviceNoMail()
        }
    }
    
    func sendInviteFriendsMessage(presenter : UIViewController, completion : ((MessageComposeResult) -> ())? = nil) {
        self.presenter = presenter
        if MFMessageComposeViewController.canSendText() {
            self.messageCompletion = completion
            
            let message = MFMessageComposeViewController()
            message.view.tag = MailInviteTag
            message.view.tintColor = AccentColor
            message.messageComposeDelegate = self
            let messageBody = String(format: "InviteMessage".aliasLocalized,
                                     "AppStoreUrlIOS".aliasLocalized,
                                     "AppStoreUrlAndroid".aliasLocalized,
                                     !UserData.instance.firstName.isEmpty ? UserData.instance.firstName : "Bye!".localized)
            message.body = messageBody.localized
            message.navigationBar.tintColor = AccentColor
            self.presenter.present(message, animated: true)
        } else {
            self.alertDeviceNoMessage()
        }
    }
    
    func mailComposeController(_ controller: MFMailComposeViewController, didFinishWith result: MFMailComposeResult, error: Error?) {
        if let completion = mailCompletion {
            completion(result)
        }
        if controller.view.tag == MailInviteTag {
            if result == MFMailComposeResult.sent {
                UserData.instance.increaseInviteFriendSentCount()
                Analytics.instance.logInviteFriend()
            }
        } else if controller.view.tag == MailSupportTag {
            Analytics.instance.logSupportMail()
        }
        controller.dismiss(animated: true)
    }
    
    func messageComposeViewController(_ controller: MFMessageComposeViewController, didFinishWith result: MessageComposeResult) {
        if let completion = messageCompletion {
            completion(result)
        }
        if controller.view.tag == MailInviteTag {
            if result == MessageComposeResult.sent {
                UserData.instance.increaseInviteFriendSentCount()
                Analytics.instance.logInviteFriend()
            }
        }
        controller.dismiss(animated: true)
    }
    
    func alertDeviceNoMail() {
        let alertController = UIAlertController(title: "Mail Configuration".localized, message: "MailNotConfigured".aliasLocalized, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
        alertController.addAction(okAction)
        alertController.view?.tintColor = AccentColor
        self.presenter.present(alertController, animated: true, completion: nil)
    }
    
    func alertDeviceNoMessage() {
        let alertController = UIAlertController(title: "Message Configuration".localized, message: "MessageNotConfigured".aliasLocalized, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
        alertController.addAction(okAction)
        alertController.view?.tintColor = AccentColor
        self.presenter.present(alertController, animated: true, completion: nil)
    }
}
