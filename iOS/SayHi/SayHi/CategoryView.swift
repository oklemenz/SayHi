//
//  CategoryView.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 21.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

let CategoryMargin: CGFloat = 4.0 * 2
let CategoryMaxWidth: CGFloat = UIScreen.main.bounds.width - CategoryMargin
let CategoryMaxHalfWidth: CGFloat = UIScreen.main.bounds.width / 2.0 - CategoryMargin
let CategoryMaxWidthInset: CGFloat = 20.0

@objc protocol CategoryViewDelegate {
    func didPressCategory(_ button : UIButton)
}

class CategoryView: UIView {
    
    weak var delegate: CategoryViewDelegate?
    
    @IBAction func categoryPressed(_ button: UIButton) {
        self.delegate?.didPressCategory(button)
    }
    
    @IBOutlet weak var button: UIButton!
    @IBOutlet weak var nameMaxWidthConstraint: NSLayoutConstraint!
    
    var _data : Category!
    var data : Category {
        set {
            _data = newValue
            let category = data
            self.backgroundColor = category.bgColor
            self.tintColor = category.textColor
            
            if category.selected {
                self.layer.borderWidth = 1
                self.layer.borderColor = UIColor.white.cgColor
            } else {
                self.layer.borderWidth = 0
            }
            
            UIView.setAnimationsEnabled(false)
            self.button.setTitle(category.name + " ", for: .normal)
            self.button.setImage(category.iconImage ?? IconService.instance.icon(CategoryStagedIcon), for: .normal)
            self.button.imageView?.bounds = CGRect(x: 0, y: 0, width: ContentSize, height: ContentSize)
            self.button.imageView?.contentMode = .scaleAspectFit
            self.button.imageEdgeInsets = UIEdgeInsetsMake(0, 0, 0, 0)
            self.button.titleEdgeInsets = UIEdgeInsetsMake(0, 0, 0, 0)
            self.button.contentHorizontalAlignment = .left
            self.button.backgroundColor = UIColor.clear
            UIView.setAnimationsEnabled(true)
        }
        get {
            return _data
        }
    }
    
    override func awakeFromNib() {
        self.layer.cornerRadius = 2.5
        self.layer.masksToBounds = true
        self.setMaxWidthScreen()
        self.autoresizingMask = [.flexibleHeight, .flexibleWidth]
    }
    
    func setMaxWidthScreen() {
        self.nameMaxWidthConstraint.constant = CategoryMaxWidth
    }
    
    func setMaxWidthHalfScreen() {
        self.nameMaxWidthConstraint.constant = CategoryMaxHalfWidth
    }
    
    func setMaxWidthScreenWithBias(_ bias: CGFloat) {
        self.nameMaxWidthConstraint.constant = CategoryMaxWidth - bias
    }
}
