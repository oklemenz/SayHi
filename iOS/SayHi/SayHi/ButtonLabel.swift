//
//  ButtonLabel
//  SayHi
//
//  Created by Klemenz, Oliver on 13.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

@objc protocol ButtonLabelDelegate {
    func didPressButtonLabel(_ sender: ButtonLabel)
}

class ButtonLabel : UILabel {
    
    let PressedAlphaRatio: CGFloat = 2.5
    
    weak var delegate: ButtonLabelDelegate?
    var refAlpha: CGFloat = 1.0
    
    var active: Bool = true
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
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
                self.delegate?.didPressButtonLabel(self)
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
