//
//  HelpView.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 02.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class HelpView : UIView {
    
    var owner: UIViewController?
    
    var constraintsAdjusted: Bool = false
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        self.backgroundColor = .clear
        let background = UIView(frame: self.frame)
        background.autoresizingMask = [.flexibleHeight, .flexibleWidth]
        background.backgroundColor = .black
        background.alpha = 0.70
        self.insertSubview(background, at: 0)
    }
    
    func adjustConstraints() {
        if constraintsAdjusted {
            return
        }
        if isIPhoneX() {
            for constraint in constraints {
                if constraint.firstItem as? UIView == self && constraint.firstAttribute == .top {
                    constraint.constant = constraint.constant + StatusBarHeight - 20
                }
                if constraint.secondItem as? UIView == self && constraint.secondAttribute == .top {
                    constraint.constant = constraint.constant + StatusBarHeight - 20
                }
            }
        }
        constraintsAdjusted = true
    }
    
    func show(owner: UIViewController) {
        if self.owner != nil {
            return
        }
        self.owner = owner
        self.owner?.view.endEditing(true)
        if let listViewController = owner as? ListViewController {
            listViewController.scrollToTop()
        }
        adjustConstraints()
        let presenter = owner.navigationController!
        self.transform = CGAffineTransform.identity
        self.frame = CGRect(x: -1, y: -1, width: presenter.view.frame.size.width + 2, height: presenter.view.frame.size.height + 2)
        self.alpha = 0.0
        self.transform = CGAffineTransform(scaleX: 0.1, y: 0.1)
        
        UIView.animate(withDuration: 0.75, delay: 0, usingSpringWithDamping: 0.7, initialSpringVelocity: 0, options: .curveEaseInOut, animations: {
            presenter.view.addSubview(self)
            self.alpha = 1.0
            self.transform = CGAffineTransform.identity
        }, completion: nil)
    }
    
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.hide()
    }
    
    func hide() {
        UIView.animate(withDuration: 0.75, delay: 0, usingSpringWithDamping: 0.7, initialSpringVelocity: 0, options: .curveEaseInOut, animations: {
            self.alpha = 0.0
            self.transform = CGAffineTransform(scaleX: 0.1, y: 0.1)
        }, completion: { (completed) in
            self.removeFromSuperview()
            self.owner = nil
        })
    }
}
