/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import com.google.gson.JsonObject;

public interface TransactionValidator {

    boolean valid(Transaction txn) throws HandleException;

    default ValidationResult validate(Transaction txn) throws HandleException {
        if (valid(txn)) return new ValidationResult(true, null, null);
        else return new ValidationResult(false, null, null);
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final String message;
        private final JsonObject report;

        public ValidationResult(boolean isValid, String message, JsonObject report) {
            this.isValid = isValid;
            this.message = message;
            this.report = report;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getMessage() {
            return message;
        }

        public JsonObject getReport() {
            return report;
        }
    }
}
