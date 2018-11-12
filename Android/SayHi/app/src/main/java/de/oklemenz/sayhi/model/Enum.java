package de.oklemenz.sayhi.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 31.10.16.
 */

public class Enum implements Serializable {

    public enum Gender {
        None(""),
        Male("m"),
        Female("f");

        public String code;

        public static Gender fromCode(String code) {
            switch (code) {
                case "":
                    return None;
                case "m":
                    return Male;
                case "f":
                    return Female;
            }
            return None;
        }

        Gender(String code) {
            this.code = code;
        }

        public static List<Gender> list = Arrays.asList(None, Male, Female);
    }

    public enum MatchMode {
        Basic(1),
        Exact(2),
        Adapt(3),
        Tries(4),
        Open(5);

        public int code;

        public String toString() {
            switch (code) {
                case 1:
                    return "basic";
                case 2:
                    return "exact";
                case 3:
                    return "adapt";
                case 4:
                    return "tries";
                case 5:
                    return "open";
            }
            return "exact";
        }

        public String toExternalString() {
            switch (code) {
                case 1:
                    return "basic";
                case 2:
                    return "exact";
                case 3:
                    return "adapt";
                case 4:
                    return "try";
                case 5:
                    return "open";
            }
            return "exact";
        }

        public String toNumberedString() {
            return toString() + "_" + code;
        }

        public static MatchMode fromCode(int code) {
            switch (code) {
                case 1:
                    return Basic;
                case 2:
                    return Exact;
                case 3:
                    return Adapt;
                case 4:
                    return Tries;
                case 5:
                    return Open;
            }
            return Exact;
        }

        public static MatchMode fromExternalCode(String code) {
            switch (code) {
                case "basic":
                    return Basic;
                case "exact":
                    return Exact;
                case "adapt":
                    return Adapt;
                case "try":
                    return Tries;
                case "open":
                    return Open;
            }
            return Exact;
        }

        MatchMode(int code) {
            this.code = code;
        }

        public static List<MatchMode> list = Arrays.asList(Basic, Exact, Adapt, Tries, Open);
    }

    public enum PasscodeTimeout {
        Min0(0),
        Min1(1),
        Min5(5),
        Min10(10);

        public int code;

        public String toString() {
            switch (code) {
                case 0:
                    return "min0";
                case 1:
                    return "min1";
                case 5:
                    return "min5";
                case 10:
                    return "min10";
            }
            return "min5";
        }

        public static PasscodeTimeout fromCode(int code) {
            switch (code) {
                case 0:
                    return Min0;
                case 1:
                    return Min1;
                case 5:
                    return Min5;
                case 10:
                    return Min10;
            }
            return Min0;
        }

        PasscodeTimeout(int code) {
            this.code = code;
        }

        public static List<PasscodeTimeout> list = Arrays.asList(Min0, Min1, Min5, Min10);
    }

    public enum CorrectionLevel {
        Low("L"),
        Medium("M"),
        Quartile("Q"),
        High("H");

        public String code;

        public static CorrectionLevel fromCode(String code) {
            switch (code) {
                case "L":
                    return Low;
                case "M":
                    return Medium;
                case "Q":
                    return Quartile;
                case "H":
                    return High;
            }
            return Low;
        }

        CorrectionLevel(String code) {
            this.code = code;
        }

        public static List<CorrectionLevel> list = Arrays.asList(Low, Medium, Quartile, High);
    }

    public enum RelationType {
        None("none"),
        Date("date"),
        Single("single"),
        Family("family"),
        Partner("partner"),
        Wife("wife"),
        Husband("husband"),
        Parent("parent"),
        Father("father"),
        Mother("mother"),
        Child("child"),
        Son("son"),
        Daugther("daugther"),
        Sibling("sibling"),
        Brother("brother"),
        Sister("sister"),
        Grandparent("grandparent"),
        Grandfather("grandfather"),
        Grandmother("grandmother"),
        Grandchild("grandchild"),
        Grandson("grandson"),
        Granddaughter("granddaughter"),
        Relative("relative"),
        Uncle("uncle"),
        Aunt("aunt"),
        Cousins("cousins"),
        Cousin("cousin"),
        Cousine("cousine"),
        Nephew("nephew"),
        Niece("niece"),
        Friend("friend"),
        Boyfriend("boyfriend"),
        Girlfriend("girlfriend"),
        Colleague("colleague"),
        Boss("boss"),
        Manager("manager"),
        Employee("employee"),
        Peer("peer"),
        Other("other");

        public String code;

        public String toString() {
            return Utilities.uppercaseFirst(code);
        }

        public static RelationType fromCode(String code) {
            switch (code) {
                case "":
                    return None;
                case "date":
                    return Date;
                case "single":
                    return Single;
                case "family":
                    return Family;
                case "partner":
                    return Partner;
                case "wife":
                    return Wife;
                case "husband":
                    return Husband;
                case "parent":
                    return Parent;
                case "father":
                    return Father;
                case "mother":
                    return Mother;
                case "child":
                    return Child;
                case "son":
                    return Son;
                case "daugther":
                    return Daugther;
                case "sibling":
                    return Sibling;
                case "brother":
                    return Brother;
                case "sister":
                    return Sister;
                case "grandparent":
                    return Grandparent;
                case "grandfather":
                    return Grandfather;
                case "grandmother":
                    return Grandmother;
                case "grandchild":
                    return Grandchild;
                case "grandson":
                    return Grandson;
                case "granddaughter":
                    return Granddaughter;
                case "relative":
                    return Relative;
                case "uncle":
                    return Uncle;
                case "aunt":
                    return Aunt;
                case "cousins":
                    return Cousins;
                case "cousin":
                    return Cousin;
                case "cousine":
                    return Cousine;
                case "nephew":
                    return Nephew;
                case "niece":
                    return Niece;
                case "friend":
                    return Friend;
                case "boyfriend":
                    return Boyfriend;
                case "girlfriend":
                    return Girlfriend;
                case "colleague":
                    return Colleague;
                case "boss":
                    return Boss;
                case "manager":
                    return Manager;
                case "employee":
                    return Employee;
                case "peer":
                    return Peer;
                case "other":
                    return Other;
            }
            return None;
        }

        RelationType(String code) {
            this.code = code;
        }

        public static RelationType fromDescription(String description) {
            return Enum.RelationType.fromCode(Utilities.lowercaseFirst(description));
        }

        public static List<RelationType> list = Arrays.asList(
                None, Date, Single,
                Boyfriend, Girlfriend, Wife, Husband,
                Partner, Family, Friend,
                Child, Son, Daugther,
                Parent, Father, Mother,
                Sibling, Brother, Sister,
                Relative, Uncle, Aunt,
                Grandchild, Grandson, Granddaughter,
                Grandparent, Grandfather, Grandmother,
                Cousins, Cousin, Cousine,
                Nephew, Niece,
                Colleague, Boss, Manager, Employee, Peer,
                Other);
    }
}
