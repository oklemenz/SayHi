//
//  Utilities.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 19.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation
import LocalAuthentication

func vibrateEnabled() -> Bool {
    return Platform.isDevice && isIPhone()
}

func biometricsEnabled() -> Bool {
    return LAContext().canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
}

func biometricsText() -> String {
    if #available(iOS 11.0, *) {
        let laContext = LAContext()
        laContext.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
        if laContext.biometryType == .touchID {
            return "Use Touch ID".localized
        } else if laContext.biometryType == .faceID {
            return "Use Face ID".localized
        }
    }
    return "Use Touch ID".localized
}

func passcodeEnabled() -> Bool {
    return LAContext().canEvaluatePolicy(.deviceOwnerAuthentication, error: nil)
}

func authEnabled() -> Bool {
    return biometricsEnabled() || passcodeEnabled()
}

func isIPhone() -> Bool {
    return UIDevice.current.model.contains("iPhone")
}

func isIPhone5() -> Bool {
    return (Double(UIScreen.main.bounds.size.height) - 568.0) < Double.ulpOfOne
}

func isIPhoneX() -> Bool {
    if #available(iOS 11.0, *) {
        if ((UIApplication.shared.keyWindow?.safeAreaInsets.top)! > CGFloat(0.0)) {
            return true
        }
    }
    return false
}

func isIPad() -> Bool {
    return UIDevice.current.userInterfaceIdiom == .pad
}

var _dateFormatter : DateFormatter!
var dateFormatter : DateFormatter {
    if _dateFormatter == nil {
        _dateFormatter = DateFormatter()
        _dateFormatter.dateStyle = .medium
        _dateFormatter.timeStyle = .none
        _dateFormatter.doesRelativeDateFormatting = true
    }
    return _dateFormatter
}

var _longDateFormatter : DateFormatter!
var longDateFormatter : DateFormatter {
    if _longDateFormatter == nil {
        _longDateFormatter = DateFormatter()
        _longDateFormatter.dateStyle = .full
        _longDateFormatter.timeStyle = .none
        _longDateFormatter.doesRelativeDateFormatting = true
    }
    return _longDateFormatter
}

var _dateTimeFormatter : DateFormatter!
var dateTimeFormatter : DateFormatter {
    if _dateTimeFormatter == nil {
        _dateTimeFormatter = DateFormatter()
        _dateTimeFormatter.dateStyle = .short
        _dateTimeFormatter.timeStyle = .short
        _dateTimeFormatter.doesRelativeDateFormatting = true
    }
    return _dateTimeFormatter
}

var _timeFormatter : DateFormatter!
var timeFormatter : DateFormatter {
    if _timeFormatter == nil {
        _timeFormatter = DateFormatter()
        _timeFormatter.dateStyle = .none
        _timeFormatter.timeStyle = .short
        _timeFormatter.doesRelativeDateFormatting = true
    }
    return _timeFormatter
}

struct Platform {
    static var isDevice: Bool {
        return TARGET_OS_SIMULATOR == 0
    }
}

extension UIColor {

    static func colorWithHexString(hexString: String, alpha:CGFloat? = 1.0) -> UIColor {
        let hexint = Int(intFromHexString(hexString))
        let red = CGFloat((hexint & 0xff0000) >> 16) / 255.0
        let green = CGFloat((hexint & 0xff00) >> 8) / 255.0
        let blue = CGFloat((hexint & 0xff) >> 0) / 255.0
        let alpha = alpha!
        let color = UIColor(red: red, green: green, blue: blue, alpha: alpha)
        return color
    }
    
    private static func intFromHexString(_ hexString: String) -> UInt32 {
        var hexInt: UInt32 = 0
        let scanner: Scanner = Scanner(string: hexString)
        scanner.charactersToBeSkipped = CharacterSet(charactersIn: "#")
        scanner.scanHexInt32(&hexInt)
        return hexInt
    }
    
    func isLight() -> Bool {
        let components = self.cgColor.components!
        let brightness = (components[0] * 299 + components[1] * 587 + components[2] * 114) / 1000
        return brightness >= 0.75
    }
    
    func lighterColor() -> UIColor {
        var r:CGFloat = 0, g:CGFloat = 0, b:CGFloat = 0, a:CGFloat = 0
        
        if self.getRed(&r, green: &g, blue: &b, alpha: &a){
            return UIColor(red: min(r + 0.2, 1.0), green: min(g + 0.2, 1.0), blue: min(b + 0.2, 1.0), alpha: a)
        }
        
        return self
    }
    
    func darkerColor() -> UIColor {
        var r:CGFloat = 0, g:CGFloat = 0, b:CGFloat = 0, a:CGFloat = 0
        
        if self.getRed(&r, green: &g, blue: &b, alpha: &a) {
            return UIColor(red: max(r - 0.2, 0.0), green: max(g - 0.2, 0.0), blue: max(b - 0.2, 0.0), alpha: a)
        }
        
        return self
    }
}

extension UIImageView {
    func blur() {
        self.unblur()
        let blurEffect = UIBlurEffect(style: .light)
        let blurEffectView = UIVisualEffectView(effect: blurEffect)
        blurEffectView.frame = self.bounds
        blurEffectView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        self.addSubview(blurEffectView)
    }
    
    func unblur() {
        for view in subviews {
            view.removeFromSuperview()
        }
    }
}

extension UIImage {
    func colored(_ color: UIColor) -> UIImage {
        var image = withRenderingMode(.alwaysTemplate)
        UIGraphicsBeginImageContextWithOptions(size, false, scale)
        color.set()
        image.draw(in: CGRect(x: 0, y: 0, width: size.width, height: size.height))
        image = UIGraphicsGetImageFromCurrentImageContext()!
        UIGraphicsEndImageContext()
        return image
    }

    public convenience init?(color: UIColor, size: CGSize = CGSize(width: 1, height: 1)) {
        let rect = CGRect(origin: .zero, size: size)
        UIGraphicsBeginImageContextWithOptions(rect.size, false, 0.0)
        color.setFill()
        UIRectFill(rect)
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        guard let cgImage = image?.cgImage else { return nil }
        self.init(cgImage: cgImage)
    }
    
    func blur(radius: CGFloat = 1.0) -> UIImage {
        let imageToBlur = self.ciImage
        let blurfilter = CIFilter(name: "CIGaussianBlur")
        blurfilter?.setValue(imageToBlur, forKey: "inputImage")
        blurfilter?.setValue(radius, forKey: "inputRadius")
        if let outputImage = blurfilter?.outputImage {
            let context = CIContext(options: nil)
            if let cgImage = context.createCGImage(outputImage, from: CGRect(x: 0, y: 0, width: self.size.width, height: self.size.height)) {
                return UIImage(cgImage: cgImage)
            }
        }
        return self
    }
}

extension Date {
    struct Formatter {
        static let iso: DateFormatter = {
            let formatter = DateFormatter()
            formatter.calendar = Calendar(identifier: .iso8601)
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.timeZone = TimeZone(secondsFromGMT: 0)
            formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
            return formatter
        }()
    }
    var iso: String {
        return Formatter.iso.string(from: self)
    }
    var dayDate : Date {
        let dateComponent = Calendar.current.dateComponents([.year,.month,.day], from: self)
        return Calendar.current.date(from: dateComponent)!
    }
}

extension Data {
    
    public var bytes: Array<UInt8> {
        return Array(self)
    }
}

extension String {
    var dateFromISO: Date? {
        return Date.Formatter.iso.date(from: self)
    }
    
    var isValidEmail : Bool {
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        let emailTest = NSPredicate(format:"SELF MATCHES %@", emailRegEx)
        return emailTest.evaluate(with: self)
    }
    
    var condensed: String {
        let components = self.components(separatedBy: NSCharacterSet.whitespacesAndNewlines)
        return components.filter { !$0.isEmpty }.joined(separator: " ")
    }
    
    var cleaned: String {
        return self.replacingOccurrences(of: "[^a-zA-Z0-9]", with: "_", options: .regularExpression, range: nil).condensed
    }
    
    var cleanProtected: String {
        return self.replacingOccurrences(of: "[#\\/]", with: "", options: .regularExpression, range: nil)
    }
    
    var first: String {
        return String(prefix(1))
    }
    var last: String {
        return String(suffix(1))
    }
    
    func prefixStr(_ i: Int) -> String {
        return String(prefix(i))
    }
    
    func suffixStr(_ i: Int) -> String {
        return String(suffix(i))
    }
    
    func index(from: Int) -> Index {
        return self.index(startIndex, offsetBy: from)
    }
    
    func substring(from: Int) -> String {
        let fromIndex = index(from: from)
        return String(self[fromIndex...])
    }
    
    func substring(to: Int) -> String {
        let toIndex = index(from: to)
        return String(self[..<toIndex])
    }
    
    func substring(with r: Range<Int>) -> String {
        let startIndex = index(from: r.lowerBound)
        let endIndex = index(from: r.upperBound)
        return String(self[startIndex..<endIndex])
    }
    
    var uppercaseFirst: String {
        return first.uppercased() + dropFirst()
    }
    
    var capitalize: String {
        var parts = self.condensed.components(separatedBy: .whitespacesAndNewlines)
        parts = parts.map { (part) -> String in
            return part.uppercaseFirst
        }
        return parts.joined(separator: " ")
    }

    var localized: String {
        return NSLocalizedString(self, tableName: nil, bundle: Bundle.main, value: "", comment: "")
    }
    
    var aliasLocalized: String {
        return localized
    }
    
    var codeLocalized: String {
        return localized
    }
    
    var fileLocalized: String {
        return localized
    }
    
    func termLocalized(_ arguments: CVarArg...) -> String {
        var name = self
        if let terminology = Settings.instance.terminology {
            name = name + "_" + terminology
        } else if Settings.instance.leftLabel != nil || Settings.instance.rightLabel != nil {
            return ""
        }
        let text = name.aliasLocalized
        if text == name {
            return ""
        }
        return String(format: text, arguments: arguments)
    }
    
    func searchNormalized(langCode: String) -> String {
        let locale = Locale(identifier: langCode)
        var searchNormalized = self.lowercased()
        searchNormalized = searchNormalized.folding(options: [.diacriticInsensitive, .widthInsensitive, .caseInsensitive], locale: locale)
        searchNormalized = searchNormalized.components(separatedBy: CharacterSet.alphanumerics.inverted).joined(separator: "")
        if let regex = try? NSRegularExpression(pattern: "(.)\\1+", options: .caseInsensitive) {
            searchNormalized = regex.stringByReplacingMatches(in: searchNormalized, options: [], range: NSRange(0..<searchNormalized.count), withTemplate: "$1")
        }
        searchNormalized = searchNormalized.trimmingCharacters(in: .whitespacesAndNewlines)
        if !searchNormalized.isEmpty {
            return searchNormalized
        }
        return self.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
    
    func localizedWithLang(langId: String, langCode: String, value: String?) -> String {
        var text : String?
        let path = Bundle.main.path(forResource: UserData.instance.langCode, ofType: "lproj")
        if let path = path {
            let languageBundle = Bundle(path: path)
            text = languageBundle?.localizedString(forKey: self, value: "", table: nil)
            if text != nil {
                if value != nil {
                    return String(format: text!, value!)
                } else {
                    return String(format: text!)
                }
            }
        }
        if value != nil {
            return String.localizedStringWithFormat(self, value!)
        } else {
            return String.localizedStringWithFormat(self)
        }
    }
    
    static func random(length: Int) -> String {
        let charSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var c = charSet.map { String($0) }
        var s:String = ""
        for _ in (1...length) {
            s.append(c[Int(arc4random()) % c.count])
        }
        return s
    }
    
    static func randomString(_ length: Int) -> String {
        let letters : NSString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let len = UInt32(letters.length)
        var randomString = ""
        for _ in 0 ..< length {
            let rand = arc4random_uniform(len)
            var nextChar = letters.character(at: Int(rand))
            randomString += NSString(characters: &nextChar, length: 1) as String
        }
        return randomString
    }
}

extension UINavigationController {
    
    open override var childForStatusBarHidden: UIViewController? {
        return self.topViewController
    }
    
    open override var childForStatusBarStyle: UIViewController? {
        return self.topViewController
    }
}

extension UIViewController {
    
    var embedded: UINavigationController {
        return UINavigationController(rootViewController: self)
    }
    
    var activeViewController: UIViewController {
        if let nav = self as? UINavigationController {
            if let top = nav.topViewController {
                return top.activeViewController
            }
        }
        if let tab = self as? UITabBarController {
            if let selected = tab.selectedViewController {
                return selected.activeViewController
            }
        }
        if let presented = self.presentedViewController {
            if !presented.isBeingDismissed {
                return presented.activeViewController
            }
        }
        return self
    }
}

extension UIApplication {
    class func topViewController(base: UIViewController? = UIApplication.shared.keyWindow?.rootViewController) -> UIViewController? {
        if let nav = base as? UINavigationController {
            return topViewController(base: nav.visibleViewController)
        }
        if let tab = base as? UITabBarController {
            if let selected = tab.selectedViewController {
                return topViewController(base: selected)
            }
        }
        if let presented = base?.presentedViewController {
            return topViewController(base: presented)
        }
        return base
    }
}

extension UIScreen {
    var isRetina: Bool {
        return screenScale >= 2.0
    }
    
    var isRetinaHD: Bool {
        return screenScale >= 3.0
    }
    
    var screenScale: CGFloat {
        return UIScreen.main.scale == 1.0 ? 1.0 : 2.0
    }
}

extension NSLayoutConstraint {
    func setMultiplier(multiplier: CGFloat) -> NSLayoutConstraint {
        NSLayoutConstraint.deactivate([self])
        let newConstraint = NSLayoutConstraint(
            item: firstItem!,
            attribute: firstAttribute,
            relatedBy: relation,
            toItem: secondItem,
            attribute: secondAttribute,
            multiplier: multiplier,
            constant: multiplier > 0 ? constant : 0)
        newConstraint.priority = priority
        newConstraint.shouldBeArchived = self.shouldBeArchived
        newConstraint.identifier = self.identifier
        newConstraint.isActive = true
        NSLayoutConstraint.activate([newConstraint])
        return newConstraint
    }
    
    func setConstant(constant: CGFloat) -> NSLayoutConstraint {
        NSLayoutConstraint.deactivate([self])
        let newConstraint = NSLayoutConstraint(
            item: firstItem!,
            attribute: firstAttribute,
            relatedBy: .equal,
            toItem: nil,
            attribute: .notAnAttribute,
            multiplier: 1.0,
            constant: constant)
        newConstraint.priority = priority
        newConstraint.shouldBeArchived = self.shouldBeArchived
        newConstraint.identifier = self.identifier
        newConstraint.isActive = true
        NSLayoutConstraint.activate([newConstraint])
        return newConstraint
    }
    
    func setFirstItem(_ firstItem: Any) -> NSLayoutConstraint {
        NSLayoutConstraint.deactivate([self])
        let newConstraint = NSLayoutConstraint(
            item: firstItem,
            attribute: firstAttribute,
            relatedBy: relation,
            toItem: secondItem,
            attribute: secondAttribute,
            multiplier: multiplier,
            constant: constant)
        newConstraint.priority = priority
        newConstraint.shouldBeArchived = self.shouldBeArchived
        newConstraint.identifier = self.identifier
        newConstraint.isActive = true
        NSLayoutConstraint.activate([newConstraint])
        return newConstraint
    }
    
    func setSecondItem(_ secondItem: Any) -> NSLayoutConstraint {
        NSLayoutConstraint.deactivate([self])
        let newConstraint = NSLayoutConstraint(
            item: firstItem!,
            attribute: firstAttribute,
            relatedBy: relation,
            toItem: secondItem,
            attribute: secondAttribute,
            multiplier: multiplier,
            constant: constant)
        newConstraint.priority = priority
        newConstraint.shouldBeArchived = self.shouldBeArchived
        newConstraint.identifier = self.identifier
        newConstraint.isActive = true
        NSLayoutConstraint.activate([newConstraint])
        return newConstraint
    }
}

extension URL {
    public var queryItems: [String: String] {
        var params = [String: String]()
        return URLComponents(url: self, resolvingAgainstBaseURL: false)?
            .queryItems?
            .reduce([:], { (_, item) -> [String: String] in
                params[item.name] = item.value
                return params
            }) ?? [:]
    }
}

func += <K, V> ( left: inout [K:V], right: [K:V]) {
    for (k, v) in right {
        left.updateValue(v, forKey: k)
    }
}
