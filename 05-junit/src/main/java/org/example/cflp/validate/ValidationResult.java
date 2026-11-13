package org.example.cflp.validate;

import java.util.List;

public record ValidationResult(boolean feasible, double objective, List<String> violations) {
}
