//
//  TagView.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 21.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

let TagMargin: CGFloat = 4.0 * 2
let TagMaxWidth: CGFloat = UIScreen.main.bounds.width - TagMargin
let TagMaxHalfWidth: CGFloat = UIScreen.main.bounds.width / 2.0 - TagMargin
let TagMaxWidthInset: CGFloat = 20.0

class TagView: UIView {

    @IBOutlet weak var button: UIButton!
    @IBOutlet weak var nameMaxWidthConstraint: NSLayoutConstraint!

    var _data : Tag!
    var data : Tag {
        set {
            _data = newValue
            let tag = data
            self.backgroundColor = tag.category?.bgColor ?? UIColor.colorWithHexString(hexString: CategoryStagedColor)
            self.tintColor = tag.category?.textColor ?? UIColor.white
            
            if tag.selected {
                self.layer.borderWidth = 1
                self.layer.borderColor = UIColor.white.cgColor
            } else {
                self.layer.borderWidth = 0
            }
            
            UIView.setAnimationsEnabled(false)
            self.button.setTitle(tag.name + " ", for: .normal)
            self.button.setImage(tag.category?.iconImage ?? IconService.instance.icon(TagStagedIcon), for: .normal)
            self.button.imageView?.bounds = CGRect(x: 0, y: 0, width: ContentSize, height: ContentSize)
            self.button.imageView?.contentMode = .scaleAspectFit
            self.button.imageEdgeInsets = UIEdgeInsets.init(top: 0, left: 0, bottom: 0, right: 0)
            self.button.titleEdgeInsets = UIEdgeInsets.init(top: 0, left: 0, bottom: 0, right: 0)
            self.button.contentHorizontalAlignment = .left
            UIView.setAnimationsEnabled(true)
        }
        get {
            return _data
        }
    }
    
    override func awakeFromNib() {
        self.layer.cornerRadius = self.bounds.height / 2.0
        self.layer.masksToBounds = true
        self.isUserInteractionEnabled = false
        self.setMaxWidthScreen()
        self.autoresizingMask = [.flexibleHeight, .flexibleWidth]
    }
    
    func setMaxWidthScreen() {
        self.nameMaxWidthConstraint.constant = TagMaxWidth
    }
    
    func setMaxWidthHalfScreen() {
        self.nameMaxWidthConstraint.constant = TagMaxHalfWidth
    }
    
    func setMaxWidthScreenWithBias(_ bias: CGFloat) {
        self.nameMaxWidthConstraint.constant = TagMaxWidth - bias
    }
}
