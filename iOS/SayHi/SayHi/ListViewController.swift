//
//  ListViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

let NavLineTag : Int = 99
let BlurAlphaRatio : CGFloat = 15
let CellSelectionImageView = UIImageView(image: UIImage(color: UIColor(red: 0, green: 0, blue: 0, alpha: 0.25)))
let TableLineColor = UIColor(red: 1, green: 1, blue: 1, alpha: 0.5)

class ListViewController: UITableViewController {

    var backgroundLayer: BackgroundLayer?
    var helpButton: UIBarButtonItem?
    var helpButtonIndex: Int?
    var helpVisible: Bool = true
    
    var scrollDrag: Bool = false
    var scrollForce: Bool = false
    
    var navigationBarBackground: UIVisualEffectView?
    var navigationBarLine: UIView?
    var navigationBarLineVisible: Bool = false
    
    var sectionBackgroundClear: Bool = true
    var autoDeselectCell: Bool = true
    var fadingLocked: Bool = false
    var firstAppeared: Bool = false
    var disappears: Bool = false
    
    var activityIndicatorTop: CGFloat = 20
    var activityIndicatorView: UIActivityIndicatorView?
    var activityIndicatorActive: Bool = false
    var _activityIndicator: Bool = false
    var activityIndicator: Bool {
        set {
            _activityIndicator = newValue
            if activityIndicator {
                if self.activityIndicatorView == nil {
                    self.activityIndicatorView = UIActivityIndicatorView(style: .white)
                    self.activityIndicatorView!.translatesAutoresizingMaskIntoConstraints = false
                    self.activityIndicatorView!.isHidden = true
                    self.view.addSubview(self.activityIndicatorView!)
                    self.view.addConstraint(NSLayoutConstraint(
                        item: self.activityIndicatorView!,
                        attribute: .centerX,
                        relatedBy: .equal,
                        toItem: self.view,
                        attribute: .centerX,
                        multiplier: 1.0,
                        constant: 0))
                    self.view.addConstraint(NSLayoutConstraint(
                        item: self.activityIndicatorView!,
                        attribute: .topMargin,
                        relatedBy: .equal,
                        toItem: self.view,
                        attribute: .top,
                        multiplier: 1.0,
                        constant: activityIndicatorTop))
                    if activityIndicatorActive {
                        self.startActivityIndicator()
                    }
                }
            } else {
                self.activityIndicatorView?.removeFromSuperview()
                self.activityIndicatorView = nil
            }
        }
        get {
            return _activityIndicator
        }
    }
    
    func startActivityIndicator() {
        activityIndicatorActive = true
        if let activityIndicatorView = self.activityIndicatorView {
            activityIndicatorView.alpha = 0.0
            activityIndicatorView.isHidden = false
            activityIndicatorView.startAnimating()
            UIView.animate(withDuration: 0.5, animations: {
                activityIndicatorView.alpha = 1.0
            }, completion: nil)
        }
    }
    
    func stopActivityIndicator() {
        activityIndicatorActive = false
        if let activityIndicatorView = self.activityIndicatorView {
            UIView.animate(withDuration: 0.1, animations: {
                activityIndicatorView.alpha = 0.0
            }, completion: { (completed) in
                activityIndicatorView.isHidden = true
                activityIndicatorView.stopAnimating()
            })
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        
        backgroundLayer = BackgroundLayer()
        backgroundLayer!.frame = self.view.bounds
        self.tableView.backgroundView = UIView(frame: self.view.bounds)
        self.tableView.backgroundView?.layer.insertSublayer(backgroundLayer!, at: 0)
        
        self.navigationController?.navigationBar.setBackgroundImage(UIImage(), for: .default)
        self.navigationController?.navigationBar.shadowImage = UIImage()
        self.navigationController?.navigationBar.isTranslucent = true
        self.navigationController?.view.backgroundColor = UIColor.clear
        
        self.navigationController?.navigationBar.titleTextAttributes = [NSAttributedString.Key.foregroundColor: AccentColor]
        
        for view in self.navigationController!.view!.subviews {
            if let view = view as? UIVisualEffectView {
                navigationBarBackground = view
            }
            if view.tag == NavLineTag {
                navigationBarLine = view
            }
        }
        
        if navigationBarBackground == nil {
            navigationBarBackground = UIVisualEffectView(effect: UIBlurEffect(style: .light))
            navigationBarBackground!.frame = CGRect(x: 0, y: 0, width: self.view.bounds.size.width,
                                                    height: NavBarHeight + StatusBarHeight)
            self.navigationController?.view.insertSubview(navigationBarBackground!, belowSubview: self.navigationController!.navigationBar)
        }
        if navigationBarLine == nil {
            navigationBarLine = UIView(frame: CGRect(x: 0, y: NavBarHeight + StatusBarHeight, width: self.view.bounds.size.width, height: 1))
            navigationBarLine?.tag = NavLineTag
            navigationBarLine?.backgroundColor = TableLineColor
            self.navigationController?.view.insertSubview(navigationBarLine!, aboveSubview: navigationBarBackground!)
        }
        
        self.tableView.separatorStyle = .singleLine
        self.tableView.separatorColor = TableLineColor
        
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
    
    @objc func refreshTriggered(_ sender: Any) {
        self.refreshControl?.endRefreshing()
        self.tableView.reloadData()
    }
    
    func enableRefresh(_ enabled : Bool) {
        if enabled {
            self.refreshControl = UIRefreshControl()
            self.refreshControl?.backgroundColor = .clear
            self.refreshControl?.tintColor = AccentColor
            self.refreshControl?.addTarget(self, action: #selector(refreshTriggered), for: .valueChanged)
        } else {
            self.refreshControl = nil
        }
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
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = super.tableView(tableView, cellForRowAt: indexPath)
        cell.detailTextLabel?.textColor = AccentColor
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if autoDeselectCell {
            self.tableView.deselectRow(at: indexPath, animated: true)
        }
        
        let cell = super.tableView(tableView, cellForRowAt: indexPath)
        for view in cell.contentView.subviews {
            if let textField = view as? UITextField {
                DispatchQueue.main.async() {
                    textField.becomeFirstResponder()
                }
                return
            }
        }
    }
    
    override func tableView(_ tableView: UITableView, willDisplayHeaderView view: UIView, forSection section: Int) {
        if let view = view as? UITableViewHeaderFooterView {
            view.textLabel?.textColor = AccentColor
            if sectionBackgroundClear {
                view.backgroundView?.backgroundColor = .clear
            } else {
                view.backgroundView = UIVisualEffectView(effect: UIBlurEffect(style: .light))
            }
        }
    }
    
    func scrollToTop() {
        scrollForce = true
        var contentOffsetY = -tableView.contentInset.top
        if #available(iOS 11.0, *) {
            contentOffsetY = contentOffsetY - StatusBarHeight - NavBarHeight
        }
        tableView.setContentOffset(CGPoint(x: 0, y: contentOffsetY), animated: true)
    }
    
    override func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        scrollDrag = true
    }

    override func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate {
            scrollDrag = false
        }
    }
    
    override func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        scrollDrag = false
    }
    
    override func scrollViewDidScroll(_ scrollView: UIScrollView) {
        if disappears {
            return
        }
        if scrollDrag || scrollForce {
            DispatchQueue.main.async() {
                self.setScrollAlpha(scrollView)
                self.setFadeAlpha(scrollView, animated: true)
            }
        }
    }
    
    override func scrollViewDidEndScrollingAnimation(_ scrollView: UIScrollView) {
        scrollDrag = false
        scrollForce = false
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        disappears = false
        fadingLocked = false
        setScrollAlpha(self.tableView, viewAppears: !firstAppeared)
        setFadeAlpha(self.tableView, viewAppears: !firstAppeared)
        firstAppeared = true
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        disappears = true
        if !fadingLocked {
            UIView.animate(withDuration: 0.25, delay: 0, options: .curveLinear, animations: {
                self.navigationBarBackground?.alpha = 0
                self.navigationBarLine?.alpha = 0
            }, completion: nil)
        }
    }
    
    func setScrollAlpha(_ scrollView : UIScrollView? = nil, viewAppears: Bool = false) {
        if let scrollView = scrollView {
            var contentOffsetY = scrollView.contentInset.top + scrollView.contentOffset.y
            if #available(iOS 11.0, *) {
                if (!viewAppears) {
                    contentOffsetY = contentOffsetY + StatusBarHeight + NavBarHeight
                }
            }
            var alpha = contentOffsetY / BlurAlphaRatio
            if alpha < 0 {
                alpha = 0
            }
            if alpha > 1 {
                alpha = 1
            }
            navigationBarBackground?.alpha = alpha
        }
    }
    
    func setFadeAlpha(_ scrollView : UIScrollView? = nil, viewAppears: Bool = false, animated: Bool = false) {
        if let scrollView = scrollView {
            var contentOffsetY = scrollView.contentInset.top + scrollView.contentOffset.y
            if #available(iOS 11.0, *) {
                if (!viewAppears) {
                    contentOffsetY = contentOffsetY + StatusBarHeight + NavBarHeight
                }
            }
            if !animated {
                if contentOffsetY == 0 {
                    navigationBarLineVisible = false
                    navigationBarLine?.alpha = 0
                } else {
                    navigationBarLineVisible = true
                    navigationBarLine?.alpha = 1
                }
            } else {
                if contentOffsetY == 0 {
                    if navigationBarLineVisible {
                        navigationBarLineVisible = false
                        UIView.animate(withDuration: 0.25, delay: 0, options: .beginFromCurrentState, animations: {
                            self.navigationBarLine?.alpha = 0
                        }, completion: nil)
                    }
                } else {
                    if !navigationBarLineVisible {
                        navigationBarLineVisible = true
                        UIView.animate(withDuration: 0.25, delay: 0, options: .beginFromCurrentState, animations: {
                            self.navigationBarLine?.alpha = 1
                        }, completion: nil)
                    }
                }
            }
        }
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
    
    override var prefersStatusBarHidden: Bool {
        return false
    }
}
