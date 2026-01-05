package logic;

import entities.UserData;
import enums.UserType;
import java.util.ArrayList;

public class UserDataMapper {

    public static UserData fromArray(ArrayList<String> row) {
        String name = row.get(0);
        String email = row.get(1);
        String phone = row.get(2);
        String userMemberCode = row.get(3);
        UserType userType = UserType.valueOf(row.get(4));
        return new UserData(name,email , phone, userMemberCode, userType);
    }
}