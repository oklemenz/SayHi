//
//  AppDelegate.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

/*
 * Open by URI:
 * sayhi://?space=sap&accessCode=0a95adbf8581859ae0cc477127abeaf4ad89916405c41855af8fbc482e1634e8&language=en&status=Test&matchMode=open&handshake=true&profile=R%26I&profileRelation=Colleague&profileMatchMode=try
 */

import UIKit
import Firebase
import FirebaseAuth
import MessageUI
import CoreLocation

let AppName = "SAY Hi!"
let Namespace = "de.oklemenz.SayHi"
let AppSharedAPIKey : String = "9d159156-bcdd-4dbc-9016-df6fcfd30964"
let PrimaryLangCode: String = "en"
let BundleLangCodes: [String] = ["en", "de"]
let SeparatorString = " • "

let StatusBarHeight: CGFloat = UIApplication.shared.statusBarFrame.height
let NavBarHeight: CGFloat = 44

let LoginNotification = Notification.Name("LoginNotification")
let DataServiceSetupNotification = Notification.Name("DataServiceSetupNotification")
let SpaceSwitchedNotification = Notification.Name("SpaceSwitchedNotification")
let ColorsSetNotification = Notification.Name("ColorsSetNotification")
let SettingsFetchedNotification = Notification.Name("SettingsFetchedNotification")
let IconsFetchedNotification = Notification.Name("IconsFetchedNotification")
let FavoriteCategoriesFetchedNotification = Notification.Name("FavoriteCategoriesFetchedNotification")
let ShowContentNotification = Notification.Name("ShowContentNotification")
let SetupEndNotification = Notification.Name("SetupEndNotification")
let QRCodeRecognizedNotification = Notification.Name("QRCodeRecognizedNotification")
let UserDataFetchNotification = Notification.Name("UserDataFetchNotification")
let UserDataFetchedNotification = Notification.Name("UserDataFetchedNotification")
let UserDataChangedNotification = Notification.Name("UserDataChangedNotification")
let UserDataStoredNotification = Notification.Name("UserDataStoredNotification")
let UserDataClearedNotification = Notification.Name("UserDataClearedNotification")
let UserDataLangChangedNotification = Notification.Name("UserDataLangChangedNotification")
let UserDataMatchNotification = Notification.Name("UserDataMatchNotification")
let UpdateLocationNotification = Notification.Name("UpdateLocationNotification")

let AccentColorDefault = "#385B7D"
let GradientColor1Default = "#89BDAB"
let GradientColor2Default = "#E8DFB3"

var AccentColor: UIColor = .black
var GradientColor1: UIColor = .white
var GradientColor2: UIColor = .white

var uid : String = ""

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, CLLocationManagerDelegate {

    static let instance = AppDelegate()
    
    var window: UIWindow?
    var coverView: UIView!
    var maintenanceLabel: UILabel!
    var coverVisible: Bool = false
    
    var backgroundTaskIdentifier: UIBackgroundTaskIdentifier?
    var clearTimer: Timer?
    var protectImmediately: Bool = false
    
    let locationManager = CLLocationManager()
    
    var spaceItems: [String: String]?
    var configItems: [String: String]?
    var settingsItems: [String: String]?
    var settingsSpaceSwitch: Bool = false
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        _ = DataService.instance
        _ = SecureStore.space
        FirebaseApp.configure()
        Auth.auth().signInAnonymously() { (user, error) in
            if error != nil {
                uid = ""
            } else {
                uid = user!.uid
                NotificationCenter.default.post(name: LoginNotification, object: nil)
                IconService.instance.fetch()
            }
            
            FirebaseAnalytics.Analytics.setUserProperty(Locale.current.languageCode ?? "", forName: "deviceLanguage")
            FirebaseAnalytics.Analytics.setUserProperty(Locale.current.identifier, forName: "deviceLocale")
        }
        
        self.window?.tintColor = UIColor.white
        updateColors()
      
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let coverViewController = storyboard.instantiateViewController(withIdentifier: "coverView")
        self.coverView = coverViewController.view.subviews.first!
        self.maintenanceLabel = self.coverView.viewWithTag(1) as! UILabel
        
        NotificationCenter.default.addObserver(self, selector: #selector(dataServiceSetup), name: DataServiceSetupNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(settingsFetched), name: SettingsFetchedNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(userDataFetched), name: UserDataFetchedNotification, object: nil)
        
        return true
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    func application(_ app: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any] = [:]) -> Bool {
        spaceItems = url.queryItems
        configItems = url.queryItems
        
        if DataService.instance.isSetup {
            dataServiceSetup()
        }
        if !SecureStore.appInitialized() || UserData.instance.initialized {
            applyConfiguration()
        } else {
            UserData.instance.requestInitialize = true
            NotificationCenter.default.post(name: ShowContentNotification, object: nil)
        }

        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
        self.stopLocation()
        self.window?.endEditing(true)
        
        showCover()
        
        let passcodeTimeout = UserData.instance.passcodeTimeout
        protectImmediately = passcodeTimeout == PasscodeTimeout.min0
        
        let protect = {
            if self.protectImmediately {
                self.clear()
            } else {
                self.clearTimer = Timer(timeInterval: Double(passcodeTimeout.rawValue), target: self, selector: #selector(self.clear), userInfo: nil, repeats: false)
            }
            self.backgroundTaskIdentifier = application.beginBackgroundTask {
                self.clearTimer?.fire()
                if let backgroundTaskIdentifier = self.backgroundTaskIdentifier {
                    application.endBackgroundTask(backgroundTaskIdentifier)
                }
                self.backgroundTaskIdentifier = UIBackgroundTaskInvalid
            }
        }
        UserData.instance.touch(error: protect, completion: protect)
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
        hideCover()
        
        if self.backgroundTaskIdentifier != UIBackgroundTaskInvalid {
            if let backgroundTaskIdentifier = self.backgroundTaskIdentifier {
                UIApplication.shared.endBackgroundTask(backgroundTaskIdentifier)
            }
            self.backgroundTaskIdentifier = UIBackgroundTaskInvalid
            
            self.clearTimer?.invalidate()
            self.clearTimer = nil
        }
        
        if !UserData.instance.initialized || protectImmediately {
            self.returnToHome()
            if !SecureStore.touchIDUsed {
                NotificationCenter.default.post(name: ShowContentNotification, object: nil)
            }
        }
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }
    
    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }
    
    @objc func settingsFetched() {
        self.settingsItems = Settings.instance.configItems
        self.settingsSpaceSwitch = Settings.instance.spaceSwitch
        if !SecureStore.appInitialized() || UserData.instance.initialized {
            applyConfiguration()
        }
        updateColors()
        
        if Settings.instance.maintenance {
            showCover()
            maintenanceLabel.isHidden = false
        } else {
            hideCover()
        }
    }
    
    func updateColors() {
        AccentColor = UIColor.colorWithHexString(hexString: Settings.instance.accentColor, alpha: 1.0)
        GradientColor1 = UIColor.colorWithHexString(hexString: Settings.instance.gradientColor1, alpha: 1.0)
        GradientColor2 = UIColor.colorWithHexString(hexString: Settings.instance.gradientColor2, alpha: 1.0)
        
        UINavigationBar.appearance().titleTextAttributes = [NSAttributedStringKey.foregroundColor: AccentColor]
        UISwitch.appearance().onTintColor = AccentColor
        UISegmentedControl.appearance().tintColor = AccentColor
        
        NotificationCenter.default.post(name: ColorsSetNotification, object: nil)
    }
    
    @objc func dataServiceSetup() {
        if let configItems = spaceItems {
            self.spaceItems = nil
            if let space = configItems["space"] {
                let spaceRefName = space.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                if !spaceRefName.isEmpty {
                    _ = DataService.instance.fetchSpaceMeta(space: spaceRefName).then(execute: { (meta) -> Void in
                        if let meta = meta {
                            if meta["protected"] as? Bool == true {
                                if let accessCode = configItems["accessCode"] {
                                    _ = DataService.instance.verifySpaceProtection(space: spaceRefName, accessCode: accessCode).then(execute: { (verified) -> Void in
                                        if verified {
                                            SecureStore.switchSpace(space)
                                            self.returnToHome()
                                        }
                                    })
                                }
                            } else {
                                SecureStore.switchSpace(space)
                                self.returnToHome()
                            }
                        }
                    })
                }
            }
        }
    }
    
    @objc func userDataFetched() {
        applyConfiguration()
    }
 
    func applyConfiguration() {
        var configChanged = false
        
        if let configItems = self.configItems {
            if applyConfigItems(configItems) {
                configChanged = true
            }
            self.configItems = nil
        }
        if let settingItems = self.settingsItems {
            _ = applyConfigItems(settingItems, fromSettings: true)
            self.settingsItems = nil
            self.settingsSpaceSwitch = false
        }
        
        if configChanged {
            if SecureStore.appInitialized() {
                UserData.instance.touch()
                returnToHome()
            }
            let alertController = UIAlertController(title: "SAY Hi!".localized, message: "ConfigurationAdjusted".aliasLocalized, preferredStyle: .alert)
            let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
            })
            alertController.addAction(okAction)
            alertController.view?.tintColor = AccentColor
            self.window?.rootViewController?.activeViewController.present(alertController, animated: true, completion: nil)
        }
    }
    
    func applyConfigItems(_ configItems: [String: String], fromSettings: Bool = false) -> Bool {
        var configChanged = false
        
        if let language = configItems["language"] {
            if fromSettings || !Settings.instance.disableSettingsLanguage {
                if !language.isEmpty && Locale.isoLanguageCodes.contains(language) {
                    if UserData.instance.langCode != language {
                        UserData.instance.langCode = language
                        NotificationCenter.default.post(name: UserDataLangChangedNotification, object: nil)
                        configChanged = true
                    }
                }
            }
        }
        
        if let status = configItems["status"] {
            if UserData.instance.status != status {
                UserData.instance.status = status
                configChanged = true
            }
        }
        
        if let matchModeExternalCode = configItems["matchMode"] {
            if fromSettings || !Settings.instance.disableSettingsMatchMode {
                if let matchMode = MatchMode.fromDescription(matchModeExternalCode) {
                    if UserData.instance.matchMode != matchMode {
                        UserData.instance.matchMode = matchMode
                        configChanged = true
                    }
                }
            }
        }
        
        if let handshakeString = configItems["handshake"] {
            if fromSettings || !Settings.instance.disableSettingsHandshake {
                let matchHandshake = handshakeString.lowercased() == "true"
                if UserData.instance.matchHandshake != matchHandshake {
                    UserData.instance.matchHandshake = matchHandshake
                    configChanged = true
                }
            }
        }
        
        if let profileName = configItems["profile"] {
            if !profileName.isEmpty {
                let profile = UserData.instance.createProfile(name: profileName)
                UserData.instance.setCurrentProfile(profile)
                configChanged = true
            }
        }
        
        if let profileRelationTypeCode = configItems["profileRelation"] {
            if let relationType = RelationType(rawValue: profileRelationTypeCode) {
                if let profile = UserData.instance.currentProfile {
                    if profile.relationType != relationType {
                        profile.relationType = relationType
                        configChanged = true
                    }
                }
            }
        }
        
        if let profileMatchModeExternalCode = configItems["profileMatchMode"] {
            if let matchMode = MatchMode.fromDescription(profileMatchModeExternalCode) {
                if let profile = UserData.instance.currentProfile {
                    if profile.matchMode != matchMode {
                        profile.matchMode = matchMode
                        configChanged = true
                    }
                }
            }
        }
        
        return configChanged
    }
    
    func returnToHome() {
        self.window?.rootViewController?.dismiss(animated: false, completion: nil)
        if let navigationController = self.window?.rootViewController as? UINavigationController {
            navigationController.popToRootViewController(animated: false)
            for view in navigationController.view.subviews {
                if let helpView = view as? HelpView {
                    helpView.hide()
                }
            }
        }
    }
    
    @objc func clear() {
        UserData.instance.clear()
    }
    
    
    func showCover() {
        if !coverVisible {
            maintenanceLabel.isHidden = true
            coverView.frame = UIScreen.main.bounds
            self.window?.addSubview(coverView)
        }
        coverVisible = true
    }
    
    func hideCover() {
        if !Settings.instance.maintenance {
            if coverVisible {
                coverView.removeFromSuperview()
            }
            coverVisible = false
        }
    }
    
    func openAppInStore() {
        UIApplication.shared.openURL(URL(string: "AppStoreUrlIOS".aliasLocalized)!)
    }
    
    func startLocation() {
        if CLLocationManager.locationServicesEnabled() {
            if CLLocationManager.authorizationStatus() == .notDetermined ||
                CLLocationManager.authorizationStatus() == .authorizedWhenInUse ||
                CLLocationManager.authorizationStatus() == .authorizedAlways {
                locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
                locationManager.distanceFilter = kCLDistanceFilterNone
                locationManager.requestWhenInUseAuthorization()
                locationManager.delegate = self
                locationManager.startUpdatingLocation()
            }
        }
    }
    
    func stopLocation() {
        locationManager.stopUpdatingLocation()
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.last {
            NotificationCenter.default.post(name: UpdateLocationNotification, object: location)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Error \(error)")
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .denied {
            alertLocationError()
        }
    }
    
    func alertLocationError() {
        let alertController = UIAlertController(title: "Tag Matching".localized, message: "LocationError".aliasLocalized, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
        alertController.addAction(okAction)
        let settingsAction = UIAlertAction(title: "Settings".localized, style: .default, handler: { (action: UIAlertAction) in
            UIApplication.shared.openURL(NSURL(string:UIApplicationOpenSettingsURLString)! as URL)
        })
        alertController.addAction(settingsAction)
        alertController.view?.tintColor = AccentColor
        self.window?.rootViewController?.activeViewController.present(alertController, animated: true, completion: nil)
    }
}
