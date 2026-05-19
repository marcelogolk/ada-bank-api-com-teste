package br.com.ada.quarkus.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
@Converter(autoApply = true)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole role) {
        if (role == null) {
            return null;
        }

        return role.getValue();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (UserRole role : UserRole.values()) {
            if (role.getValue().equals(dbData)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Valor inválido para UserRole: " + dbData);
    }
}