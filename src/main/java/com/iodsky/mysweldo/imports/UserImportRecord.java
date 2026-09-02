package com.iodsky.mysweldo.imports;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserImportRecord {

    public static final String[] CSV_COLUMN_NAMES = {
            "employeeId",
            "role",
            "email",
            "password"
    };

    private String employeeId;
    private String role;
    private String email;
    private String password;

}