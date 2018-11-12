package de.oklemenz.sayhi.model;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.oklemenz.sayhi.service.SecureStore;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class Message implements Serializable {

    private static int MatchPartCount = 13;
    private static String PartSeparatorCharacter = "\0";
    private static String TagSeparatorCharacter = "\t";

    public String langCode;
    public String firstName;
    public Enum.Gender gender;
    public int birthYear;
    public String status;
    public String space;
    public String installationUUID;
    public Enum.MatchMode matchMode;
    public boolean matchHandshake;
    public List<String> posTags = new ArrayList<>();
    public List<String> negTags = new ArrayList<>();

    public Message(String langCode, String firstName, Enum.Gender gender, int birthYear, String status, String space, String installationUUID, Enum.MatchMode matchMode, boolean matchHandshake, List<String> posTags, List<String> negTags) {
        this.langCode = langCode;
        this.firstName = firstName;
        this.gender = gender;
        this.birthYear = birthYear;
        this.status = status;
        this.space = space;
        this.installationUUID = installationUUID;
        this.matchMode = matchMode;
        this.matchHandshake = matchHandshake;
        this.posTags = posTags;
        this.negTags = negTags;
    }

    @Override
    public String toString() {
        String[] parts = {
                langCode,
                Utilities.condense(firstName),
                gender.code,
                Integer.toString(birthYear),
                Utilities.condense(status),
                Utilities.condense(space),
                installationUUID,
                Integer.toString(matchMode.code),
                Integer.toString(matchHandshake ? 1 : 0),
                Integer.toString(posTags.size()),
                TextUtils.join(TagSeparatorCharacter, posTags),
                Integer.toString(negTags.size()),
                TextUtils.join(TagSeparatorCharacter, negTags)
        };
        return TextUtils.join(PartSeparatorCharacter, parts);
    }

    public static Message generate(Profile profile) {
        UserData userData = UserData.getInstance();
        return new Message(
                userData.getLangCode(),
                userData.getFirstName(),
                userData.getGender(),
                userData.getBirthYear(),
                userData.getStatus(),
                SecureStore.getSpaceRefName(),
                userData.getInstallationUUID(),
                profile.getEffectiveMatchMode(),
                userData.getMatchHandshake(),
                profile.getPosTagEffectiveKeys(),
                profile.getNegTagEffectiveKeys()
        );
    }

    public static Message parse(String text) {
        String[] parts = text.split(PartSeparatorCharacter, -1);
        if (parts.length == MatchPartCount) {
            String langCode = parts[0];
            String firstName = parts[1];
            Enum.Gender gender = Enum.Gender.fromCode(parts[2]);
            int birthYear = Integer.parseInt(parts[3]) >= UserData.BaseYear ? Integer.parseInt(parts[3]) : 0;
            String status = parts[4];
            String space = parts[5];
            String installationUUID = parts[6];
            Enum.MatchMode matchMode = Enum.MatchMode.fromCode(Integer.parseInt(parts[7]));
            boolean matchHandshake = Integer.parseInt(parts[8]) == 1;
            int posTagCount = Integer.parseInt(parts[9]);
            List<String> posTags = Arrays.asList(!TextUtils.isEmpty(parts[10]) ? parts[10].split(TagSeparatorCharacter) : new String[]{});
            int negTagCount = Integer.parseInt(parts[11]);
            List<String> negTags = Arrays.asList(!TextUtils.isEmpty(parts[12]) ? parts[12].split(TagSeparatorCharacter) : new String[]{});

            return new Message(
                    langCode,
                    firstName,
                    gender,
                    birthYear,
                    status,
                    space,
                    installationUUID,
                    matchMode,
                    matchHandshake,
                    posTags,
                    negTags
            );
        }
        return null;
    }
}