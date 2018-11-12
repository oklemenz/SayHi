package de.oklemenz.sayhi.service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Oliver Klemenz on 24.02.17.
 */

public class NotificationCenter {

    public interface Observer {
        void notify(String name, Notification notification);
    }

    public static class Notification {

        public Map<String, Object> userInfo = new HashMap<>();

        public Notification() {
        }

        public Notification(Map<String, Object> userInfo) {
            this.userInfo = userInfo;
        }
    }

    private static NotificationCenter instance = new NotificationCenter();

    public static NotificationCenter getInstance() {
        return instance;
    }

    private Map<String, ArrayList<Observer>> observers = new HashMap<>();
    private Map<String, ArrayList<WeakReference<Observer>>> weakObservers = new HashMap<>();

    public synchronized void addObserver(String notificationName, Observer observer, boolean weak) {
        if (weak) {
            ArrayList<WeakReference<Observer>> weakList = weakObservers.get(notificationName);
            if (weakList == null) {
                weakList = new ArrayList<>();
                weakObservers.put(notificationName, weakList);
            }
            weakList.add((WeakReference<Observer>) new WeakReference(observer));
        } else {
            ArrayList<Observer> list = observers.get(notificationName);
            if (list == null) {
                list = new ArrayList<>();
                observers.put(notificationName, list);
            }
            list.add(observer);
        }
    }

    public synchronized void addObserver(String notificationName, Observer observer) {
        addObserver(notificationName, observer, true);
    }

    public synchronized void removeObserver() {
        observers.clear();
        weakObservers.clear();
    }

    public synchronized void removeObserver(String notificationName) {
        observers.remove(notificationName);
        weakObservers.remove(notificationName);
    }

    public synchronized void removeObserver(Observer observer) {
        for (ArrayList<Observer> list : observers.values()) {
            if (list != null) {
                list.remove(observer);
            }
        }
        for (ArrayList<WeakReference<Observer>> weakList : weakObservers.values()) {
            if (weakList != null) {
                for (Iterator<WeakReference<Observer>> weakIterator = weakList.listIterator(); weakIterator.hasNext(); ) {
                    WeakReference<Observer> weakObserver = weakIterator.next();
                    if (weakObserver.get() != null && weakObserver.get() == observer) {
                        weakIterator.remove();
                    }
                }
            }
        }
    }

    public synchronized void removeObserver(String notificationName, Observer observer) {
        ArrayList<Observer> list = observers.get(notificationName);
        if (list != null) {
            list.remove(observer);
        }
        ArrayList<WeakReference<Observer>> weakList = weakObservers.get(notificationName);
        if (weakList != null) {
            for (Iterator<WeakReference<Observer>> weakIterator = weakList.listIterator(); weakIterator.hasNext(); ) {
                WeakReference<Observer> weakObserver = weakIterator.next();
                if (weakObserver.get() != null && weakObserver.get() == observer) {
                    weakIterator.remove();
                }
            }
        }
    }

    public synchronized void post(String notificationName) {
        post(notificationName, null);
    }

    public synchronized void post(String notificationName, Notification notification) {
        if (notification == null) {
            notification = new Notification();
        }
        ArrayList<Observer> list = observers.get(notificationName);
        if (list != null) {
            for (Observer observer : list) {
                observer.notify(notificationName, notification);
            }
        }
        ArrayList<WeakReference<Observer>> weakList = weakObservers.get(notificationName);
        if (weakList != null) {
            for (WeakReference<Observer> weakObserver : weakList) {
                if (weakObserver.get() != null) {
                    weakObserver.get().notify(notificationName, notification);
                }
            }
        }
    }
}