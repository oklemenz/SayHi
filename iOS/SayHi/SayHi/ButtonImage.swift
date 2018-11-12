//
//  ButtonImage.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 07.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

@objc protocol ButtonImageDelegate {
    func didPressButtonImage(_ sender: ButtonImage)
}

class ButtonImage: UIImageView {

    let PressedAlphaRatio: CGFloat = 2.5
    
    weak var delegate: ButtonImageDelegate?
    var refAlpha: CGFloat = 1.0
    
    var active: Bool = true
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        self.isUserInteractionEnabled = true
        setup()
    }
    
    func setup() {
        self.isUserInteractionEnabled = true
        let tapGesture = UILongPressGestureRecognizer(target: self, action: #selector(didPress))
        tapGesture.minimumPressDuration = 0
        self.addGestureRecognizer(tapGesture)
    }
    
    @objc func didPress(_ gesture : UILongPressGestureRecognizer) {
        if !active {
            return
        }
        if gesture.state == .began {
            super.alpha = refAlpha / PressedAlphaRatio
        } else if gesture.state == .ended {
            UIView.animate(withDuration: 0.2, animations: {
                super.alpha = self.refAlpha
            })
            let location = gesture.location(in: self)
            if self.bounds.contains(location) {
                self.delegate?.didPressButtonImage(self)
            }
        } else if gesture.state == .cancelled {
            UIView.animate(withDuration: 0.2, animations: {
                super.alpha = self.refAlpha
            })
        }
    }
    
    override var alpha: CGFloat {
        set {
            super.alpha = newValue
            refAlpha = newValue
        }
        get { return super.alpha }
    }
}
