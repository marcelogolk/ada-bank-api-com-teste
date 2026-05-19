package br.com.ada.quarkus.util;

public final class OutputMaskFormatter {

    private OutputMaskFormatter() {
        // Evita instanciação
    }

    public static String formatCpf(String cpf) {
        if (cpf == null) {
            return null;
        }

        if (!cpf.matches("\\d{11}")) {
            return cpf;
        }

        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    public static String formatAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return null;
        }

        if (!accountNumber.matches("\\d{10}")) {
            return accountNumber;
        }

        return accountNumber.replaceAll("(\\d{9})(\\d)", "$1-$2");
    }
}