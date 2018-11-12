//
//  DragAndDropManager.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

@objc protocol Draggable {
    func canDragAtPoint(_ point : CGPoint) -> Bool
    func representationImageAtPoint(_ point : CGPoint) -> UIView?
    func dataItemAtPoint(_ point : CGPoint) -> AnyObject?
    func dragDataItem(_ item : AnyObject) -> Void
    @objc optional func startDraggingAtPoint(_ point : CGPoint) -> Void
    @objc optional func stopDragging() -> Void
    func isAnimating() -> Bool
}

@objc protocol Droppable {
    func canDropAtRect(_ rect : CGRect) -> Bool
    func canDropInside() -> Bool
    func willMoveItem(_ item : AnyObject, inRect rect : CGRect) -> Void
    func didMoveItem(_ item : AnyObject, inRect rect : CGRect) -> Void
    func didMoveOutItem(_ item : AnyObject) -> Void
    func dropDataItem(_ item : AnyObject, atRect : CGRect) -> Void
    func isAnimating() -> Bool
}

@objc protocol DragAndDropManagerDelegate {
    func didBeginDrag(_ view: UIView, item : AnyObject) -> Void
    func didEndDrop(_ view: UIView, sourceView: UIView, item : AnyObject, didMoveOut : Bool) -> Void
}

class DragAndDropManager: NSObject, UIGestureRecognizerDelegate {
    
    fileprivate var canvas : UIView = UIView()
    fileprivate var views : [UIView] = []
    fileprivate var longPressGestureRecogniser = UILongPressGestureRecognizer()
    
    weak var delegate: DragAndDropManagerDelegate?
    
    struct Bundle {
        var offset : CGPoint = CGPoint.zero
        var sourceDraggableView : UIView
        var overDroppableView : UIView?
        var representationImageView : UIView
        var dataItem : AnyObject
        var didMoveOut : Bool = false
        
        mutating func setMovedOut() {
            didMoveOut = true
        }
    }
    var bundle : Bundle?
    var isDragging : Bool = false
    var waitForAnimation : Bool = false
    
    func makeReadOnly() {
        self.canvas.removeGestureRecognizer(self.longPressGestureRecogniser)
    }
    
    init(canvas : UIView, collectionViews : [UIView]) {
        
        super.init()
        
        self.canvas = canvas
        
        self.longPressGestureRecogniser.delegate = self
        self.longPressGestureRecogniser.minimumPressDuration = 0.3
        self.longPressGestureRecogniser.addTarget(self, action: #selector(DragAndDropManager.updateForLongPress(_:)))
        
        self.canvas.addGestureRecognizer(self.longPressGestureRecogniser)
        self.views = collectionViews
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        if isDragging {
            return false
        }
        
        for view in self.views.filter({ v -> Bool in v is Draggable})  {
            let draggable = view as! Draggable
            let touchPointInView = touch.location(in: view)
            
            if draggable.canDragAtPoint(touchPointInView) == true {
                if let representation = draggable.representationImageAtPoint(touchPointInView) {
                    representation.frame = self.canvas.convert(representation.frame, from: view)
                    representation.alpha = 0.5
                    let pointOnCanvas = touch.location(in: self.canvas)
                    let offset = CGPoint(x: pointOnCanvas.x - representation.frame.origin.x, y: pointOnCanvas.y - representation.frame.origin.y)
                    if let dataItem : AnyObject = draggable.dataItemAtPoint(touchPointInView) {
                        self.bundle = Bundle(
                            offset: offset,
                            sourceDraggableView: view,
                            overDroppableView : view is Droppable ? view : nil,
                            representationImageView: representation,
                            dataItem : dataItem,
                            didMoveOut : false
                        )
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    @objc func updateForLongPress(_ recogniser : UILongPressGestureRecognizer) -> Void {
        
        if let bundle = self.bundle {
            let pointOnCanvas = recogniser.location(in: recogniser.view)
            let sourceDraggable : Draggable = bundle.sourceDraggableView as! Draggable
            let pointOnSourceDraggable = recogniser.location(in: bundle.sourceDraggableView)
            
            switch recogniser.state {
                case .began :
                    self.canvas.addSubview(bundle.representationImageView)
                    sourceDraggable.startDraggingAtPoint?(pointOnSourceDraggable)
                    self.delegate?.didBeginDrag(bundle.sourceDraggableView, item: bundle.dataItem)
                    self.isDragging = true
                
                case .changed :
                    var repImgFrame = bundle.representationImageView.frame
                    repImgFrame.origin = CGPoint(x: pointOnCanvas.x - bundle.offset.x, y: pointOnCanvas.y - bundle.offset.y)
                    bundle.representationImageView.frame = repImgFrame
                    
                    var overlappingArea : CGFloat = 0.0
                    var mainOverView : UIView?
                    
                    for view in self.views.filter({ v -> Bool in v is Droppable }) {
                        let viewFrameOnCanvas = self.convertRectToCanvas(view.frame, fromView: view)
                        let intersectionNew = bundle.representationImageView.frame.intersection(viewFrameOnCanvas).size

                        if (intersectionNew.width * intersectionNew.height) > overlappingArea {
                            overlappingArea = intersectionNew.width * intersectionNew.width
                            mainOverView = view
                        }
                    }
                    
                    if let droppable = mainOverView as? Droppable {
                        if !waitForAnimation || !droppable.isAnimating() {
                            let rect = self.canvas.convert(bundle.representationImageView.frame, to: mainOverView)
                            if droppable.canDropAtRect(rect) {
                                
                                if mainOverView != bundle.overDroppableView {
                                    let overDroppable = (bundle.overDroppableView as! Droppable)
                                    if !waitForAnimation || !overDroppable.isAnimating() {
                                        overDroppable.didMoveOutItem(bundle.dataItem)
                                        self.bundle!.setMovedOut()
                                        if droppable.canDropInside() {
                                            droppable.willMoveItem(bundle.dataItem, inRect: rect)
                                        }
                                    } else {
                                        return
                                    }
                                }
                                self.bundle!.overDroppableView = mainOverView
                                if droppable.canDropInside() {
                                    droppable.didMoveItem(bundle.dataItem, inRect: rect)
                                }
                            }
                        }
                    }
                    
                case .ended :
                    if bundle.sourceDraggableView != bundle.overDroppableView {
                        if let droppable = bundle.overDroppableView as? Droppable {
                            sourceDraggable.dragDataItem(bundle.dataItem)
                            let rect = self.canvas.convert(bundle.representationImageView.frame, to: bundle.overDroppableView)
                            droppable.dropDataItem(bundle.dataItem, atRect: rect)
                        }
                    }
                    
                    bundle.representationImageView.removeFromSuperview()
                    sourceDraggable.stopDragging?()
                    
                    if let view = bundle.overDroppableView {
                        self.delegate?.didEndDrop(view, sourceView: bundle.sourceDraggableView, item: bundle.dataItem, didMoveOut: bundle.didMoveOut)
                    }
                    
                    self.isDragging = false
                
                case .cancelled :
                    self.isDragging = false
                
                case .failed :
                    self.isDragging = false
                
                default:
                    break
                    
            }
        }
    }
    
    // MARK: Helper Methods
    func convertRectToCanvas(_ rect : CGRect, fromView view : UIView) -> CGRect {
        var r : CGRect = rect
        var v = view
        while v != self.canvas {
            if let sv = v.superview {
                r.origin.x += sv.frame.origin.x
                r.origin.y += sv.frame.origin.y
                v = sv
                continue
            }
            break
        }
        return r
    }
}
