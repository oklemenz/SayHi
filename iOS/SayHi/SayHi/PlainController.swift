//
//  BaseController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class PlainController: UIViewController {
    
    var backgroundLayer: BackgroundLayer?
    var helpButton: UIBarButtonItem?
    var helpButtonIndex: Int?
    var helpVisible: Bool = true
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        backgroundLayer = BackgroundLayer()
        backgroundLayer!.frame = self.view.bounds
        self.view.layer.insertSublayer(backgroundLayer!, at: 0)
        
        self.navigationController?.navigationBar.setBackgroundImage(UIImage(), for: .default)
        self.navigationController?.navigationBar.shadowImage = UIImage()
        self.navigationController?.navigationBar.isTranslucent = true
        self.navigationController?.view.backgroundColor = UIColor.clear
        
        self.navigationController?.navigationBar.titleTextAttributes = [NSAttributedStringKey.foregroundColor: AccentColor]
        
        updateHelpButton()
        
        NotificationCenter.default.addObserver(self, selector: #selector(colorsSet), name: ColorsSetNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(settingsSwitched), name: SettingsFetchedNotification, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc func colorsSet() {
        backgroundLayer?.refresh()
    }
    
    @objc func settingsSwitched() {
        updateHelpButton()
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
    
    override var prefersStatusBarHidden: Bool {
        return false
    }
    
    func updateHelpButton() {
        let helpImage = UIImage(named: "help")
        if let rightBarButtonItems = self.navigationItem.rightBarButtonItems {
            if helpButton == nil {
                for barButtonItem in rightBarButtonItems {
                    if let image = barButtonItem.image {
                        if image == helpImage {
                            helpButton = barButtonItem
                            break
                        }
                    }
                }
            }
            if let helpButton = helpButton {
                if let index = rightBarButtonItems.index(of: helpButton) {
                    if Settings.instance.disableHelp {
                        self.navigationItem.rightBarButtonItems?.remove(at: index)
                        helpButtonIndex = index
                        helpVisible = false
                    }
                } else if let helpButtonIndex = helpButtonIndex {
                    if !Settings.instance.disableHelp {
                        if helpButtonIndex <= rightBarButtonItems.count {
                            self.navigationItem.rightBarButtonItems?.insert(helpButton, at: helpButtonIndex)
                        } else {
                            self.navigationItem.rightBarButtonItems?.append(helpButton)
                        }
                        self.helpButtonIndex = nil
                        helpVisible = true
                    }
                }
            }
        }
    }
    
    var navBarBackground: UIVisualEffectView? {
        for view in self.navigationController!.view!.subviews {
            if let view = view as? UIVisualEffectView {
                return view
            }
        }
        return nil
    }
    
    var navBarLine: UIView? {
        for view in self.navigationController!.view!.subviews {
            if view.tag == NavLineTag {
                return view
            }
        }
        return nil
    }
    
    func setNavBarAlpha(_ alpha: CGFloat) {
        navBarBackground?.alpha = alpha
        navBarLine?.alpha = alpha > 0 ? 1.0 : 0.0
    }
}
