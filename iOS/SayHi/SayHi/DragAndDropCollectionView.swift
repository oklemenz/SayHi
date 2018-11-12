//
//  DragAndDropCollectionView.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

@objc protocol DragAndDropCollectionViewDataSource : UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, indexPathForDataItem dataItem: AnyObject) -> IndexPath?
    func collectionView(_ collectionView: UICollectionView, dataItemForIndexPath indexPath: IndexPath) -> AnyObject
    func collectionView(_ collectionView: UICollectionView, moveDataItemFromIndexPath from: IndexPath, toIndexPath to : IndexPath) -> Void
    func collectionView(_ collectionView: UICollectionView, insertDataItem dataItem : AnyObject, atIndexPath indexPath: IndexPath) -> Void
    func collectionView(_ collectionView: UICollectionView, deleteDataItemAtIndexPath indexPath: IndexPath) -> Void
}

class DragAndDropCollectionView: UICollectionView, Draggable, Droppable {
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    var draggingPathOfCellBeingDragged : IndexPath?
    
    var iDataSource : UICollectionViewDataSource?
    var iDelegate : UICollectionViewDelegate?
    
    var suppressInsideDragAndDrop : Bool = false
    var suppressDragAndScroll : Bool = false
    var suppressReloadData: Bool = false
    
    override func awakeFromNib() {
        super.awakeFromNib()
    }
    
    override init(frame: CGRect, collectionViewLayout layout: UICollectionViewLayout) {
        super.init(frame: frame, collectionViewLayout: layout)
    }
    
    // MARK: Draggable
    func canDragAtPoint(_ point : CGPoint) -> Bool {
        guard let _ = self.dataSource as? DragAndDropCollectionViewDataSource else {
            return false
        }
        return self.indexPathForItem(at: point) != nil
    }
    
    func representationImageAtPoint(_ point : CGPoint) -> UIView? {
        var imageView : UIView?
        if let indexPath = self.indexPathForItem(at: point) {
            if let cell = self.cellForItem(at: indexPath) {
                UIGraphicsBeginImageContextWithOptions(cell.bounds.size, cell.isOpaque, 0)
                cell.layer.render(in: UIGraphicsGetCurrentContext()!)
                let img = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                imageView = UIImageView(image: img)
                imageView?.frame = cell.frame
            }
        }
        return imageView
    }
    
    func dataItemAtPoint(_ point : CGPoint) -> AnyObject? {
        var dataItem : AnyObject?
        if let indexPath = self.indexPathForItem(at: point) {
            if let dragDropDS : DragAndDropCollectionViewDataSource = self.dataSource as? DragAndDropCollectionViewDataSource {
                dataItem = dragDropDS.collectionView(self, dataItemForIndexPath: indexPath)
            }
        }
        return dataItem
    }
    
    func startDraggingAtPoint(_ point : CGPoint) -> Void {
        self.draggingPathOfCellBeingDragged = self.indexPathForItem(at: point)
        if !self.suppressReloadData {
            self.reloadData()
        }
    }
    
    func stopDragging() -> Void {
        if let idx = self.draggingPathOfCellBeingDragged {
            if let cell = self.cellForItem(at: idx) {
                cell.isHidden = false
            }
        }
        self.draggingPathOfCellBeingDragged = nil
        if !self.suppressReloadData {
            self.reloadData()
        }
    }
    
    func dragDataItem(_ item : AnyObject) -> Void {
        if let dragDropDataSource = self.dataSource as? DragAndDropCollectionViewDataSource {
            if let existingIndexPath = dragDropDataSource.collectionView(self, indexPathForDataItem: item) {
                dragDropDataSource.collectionView(self, deleteDataItemAtIndexPath: existingIndexPath)
                self.animating = true
                self.performBatchUpdates({ () -> Void in
                    self.deleteItems(at: [existingIndexPath])
                }, completion: { complete -> Void in
                        self.animating = false
                        if !self.suppressReloadData {
                            self.reloadData()
                        }
                })
            }
        }
    }
    
    // MARK: Droppable
    func canDropAtRect(_ rect : CGRect) -> Bool {
        return (self.indexPathForCellOverlappingRect(rect) != nil)
    }
    
    func canDropInside() -> Bool {
        return !self.suppressInsideDragAndDrop
    }
    
    func indexPathForCellOverlappingRect( _ rect : CGRect) -> IndexPath? {
        
        var overlappingArea : CGFloat = 0.0
        var cellCandidate : UICollectionViewCell?
        
        let visibleCells = self.visibleCells
        if visibleCells.count == 0 {
            return IndexPath(row: 0, section: 0)
        }
        
        if isHorizontal && rect.origin.x > self.contentSize.width || !isHorizontal && rect.origin.y > self.contentSize.height + self.contentInset.bottom {
            return IndexPath(row: visibleCells.count - 1, section: 0)
        }
        
        for visible in visibleCells {
            let intersection = visible.frame.intersection(rect)
            if (intersection.width * intersection.height) > overlappingArea {
                overlappingArea = intersection.width * intersection.width
                cellCandidate = visible
            }
        }
        
        if let cellRetrieved = cellCandidate {
            return self.indexPath(for: cellRetrieved)
        }
        
        return nil
    }
    
    
    fileprivate var currentInRect : CGRect?
    func willMoveItem(_ item : AnyObject, inRect rect : CGRect) -> Void {
        
        let dragDropDataSource = self.dataSource as! DragAndDropCollectionViewDataSource
        
        if let _ = dragDropDataSource.collectionView(self, indexPathForDataItem: item) {
            return
        }
        
        if let indexPath = self.indexPathForCellOverlappingRect(rect) {
            dragDropDataSource.collectionView(self, insertDataItem: item, atIndexPath: indexPath)
            self.draggingPathOfCellBeingDragged = indexPath
            self.animating = true
            self.performBatchUpdates({ () -> Void in
                self.insertItems(at: [indexPath])
            }, completion: { complete -> Void in
                self.animating = false
                if self.draggingPathOfCellBeingDragged == nil {
                    if !self.suppressReloadData {
                        self.reloadData()
                    }
                }
            })
        }
        
        currentInRect = rect
    }
    
    var isHorizontal : Bool {
        return (self.collectionViewLayout as? UICollectionViewFlowLayout)?.scrollDirection == .horizontal
    }
    
    var animating: Bool = false
    
    func isAnimating() -> Bool {
        return animating
    }
    
    var paging : Bool = false
    func checkForEdgesAndScroll(_ rect : CGRect) -> Void {
        if suppressDragAndScroll {
            return
        }
        if paging {
            return
        }
        
        let currentRect : CGRect = CGRect(x: self.contentOffset.x, y: self.contentOffset.y, width: self.bounds.size.width, height: self.bounds.size.height)
        var rectForNextScroll : CGRect = currentRect
        
        if isHorizontal {
            let leftBoundary = CGRect(x: -30.0 + self.contentInset.left, y: 0.0, width: 30.0, height: self.frame.size.height)
            let rightBoundary = CGRect(x: self.frame.size.width - self.contentInset.right, y: 0.0, width: 30.0, height: self.frame.size.height)
            
            if rect.intersects(leftBoundary) == true {
                rectForNextScroll.origin.x -= self.bounds.size.width * 0.5
                if rectForNextScroll.origin.x < 0 {
                    rectForNextScroll.origin.x = 0
                }
            } else if rect.intersects(rightBoundary) == true {
                rectForNextScroll.origin.x += self.bounds.size.width * 0.5
                if rectForNextScroll.origin.x > self.contentSize.width - self.bounds.size.width {
                    rectForNextScroll.origin.x = self.contentSize.width - self.bounds.size.width
                }
            }
        } else {
            let topBoundary = CGRect(x: 0.0, y: -30.0 + self.contentInset.top, width: self.frame.size.width, height: 30.0)
            let bottomBoundary = CGRect(x: 0.0, y: self.frame.size.height - self.contentInset.bottom, width: self.frame.size.width, height: 30.0)
            
            if rect.intersects(topBoundary) == true {
                rectForNextScroll.origin.y -= self.bounds.size.height * 0.5
                if rectForNextScroll.origin.y < 0 {
                    rectForNextScroll.origin.y = 0
                }
            } else if rect.intersects(bottomBoundary) == true {
                rectForNextScroll.origin.y += self.bounds.size.height * 0.5
                if rectForNextScroll.origin.y > self.contentSize.height - self.bounds.size.height {
                    rectForNextScroll.origin.y = self.contentSize.height - self.bounds.size.height
                }
            }
        }
        
        if currentRect.equalTo(rectForNextScroll) == false {
            self.paging = true
            DispatchQueue.main.async() {
                self.scrollRectToVisible(rectForNextScroll, animated: true)
            }
            let delayTime = DispatchTime.now() + Double(Int64(1 * Double(NSEC_PER_SEC))) / Double(NSEC_PER_SEC)
            DispatchQueue.main.asyncAfter(deadline: delayTime) {
                self.paging = false
            }
        }
    }
    
    func didMoveItem(_ item : AnyObject, inRect rect : CGRect) -> Void {
        
        let dragDropDS = self.dataSource as! DragAndDropCollectionViewDataSource // guaranteed to have a ds
        
        if  let existingIndexPath = dragDropDS.collectionView(self, indexPathForDataItem: item),
            let indexPath = self.indexPathForCellOverlappingRect(rect) {
            if (indexPath as NSIndexPath).item != (existingIndexPath as NSIndexPath).item {
                dragDropDS.collectionView(self, moveDataItemFromIndexPath: existingIndexPath, toIndexPath: indexPath)
                self.animating = true
                self.performBatchUpdates({ () -> Void in
                    self.moveItem(at: existingIndexPath, to: indexPath)
                }, completion: { (finished) -> Void in
                    self.animating = false
                    if !self.suppressReloadData {
                        self.reloadData()
                    }
                })
                self.draggingPathOfCellBeingDragged = indexPath
            }
        }
        
        var normalizedRect = rect
        normalizedRect.origin.x -= self.contentOffset.x
        normalizedRect.origin.y -= self.contentOffset.y
        currentInRect = normalizedRect
        
        self.checkForEdgesAndScroll(normalizedRect)
    }
    
    func didMoveOutItem(_ item : AnyObject) -> Void {
        
        guard let dragDropDataSource = self.dataSource as? DragAndDropCollectionViewDataSource,
            let existngIndexPath = dragDropDataSource.collectionView(self, indexPathForDataItem: item) else {
                return
        }
        
        dragDropDataSource.collectionView(self, deleteDataItemAtIndexPath: existngIndexPath)
        
        self.animating = true
        self.performBatchUpdates({ () -> Void in
            self.deleteItems(at: [existngIndexPath])
        }, completion: { (finished) -> Void in
            self.animating = false
            if !self.suppressReloadData {
                self.reloadData()
            }
        })

        if let idx = self.draggingPathOfCellBeingDragged {
            if let cell = self.cellForItem(at: idx) {
                cell.isHidden = false
            }
        }
        
        self.draggingPathOfCellBeingDragged = nil
        
        currentInRect = nil
    }
    
    
    func dropDataItem(_ item : AnyObject, atRect : CGRect) -> Void {
        if  let index = draggingPathOfCellBeingDragged,
            let cell = self.cellForItem(at: index) , cell.isHidden == true {
            cell.alpha = 1.0
            cell.isHidden = false
        }
        
        currentInRect = nil
        self.draggingPathOfCellBeingDragged = nil
        if !self.suppressReloadData {
            self.reloadData()
        }
    }
}
