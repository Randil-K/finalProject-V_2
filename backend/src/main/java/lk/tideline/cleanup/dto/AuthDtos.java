package lk.tideline.cleanup.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lk.tideline.cleanup.model.CertificationLevel;
import lk.tideline.cleanup.model.OrganizationType;
import lk.tideline.cleanup.model.Role;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 2, max = 50, message = "must be between 2 and 50 characters")
            @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$",
                    message = "can only contain letters, spaces, apostrophes, hyphens and full stops")
            String firstName,

            @NotBlank
            @Size(min = 2, max = 50, message = "must be between 2 and 50 characters")
            @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$",
                    message = "can only contain letters, spaces, apostrophes, hyphens and full stops")
            String lastName,

            @NotBlank @Email @Size(max = 150) String email,

            @NotBlank(message = "is required")
            @Pattern(regexp = "^(\\d{12}|\\d{9}[VvXx])$",
                    message = "must be 12 digits, or 9 digits followed by V or X")
            String nic,

            @NotBlank
            @Size(min = 8, max = 100, message = "must be at least 8 characters")
            @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
                    message = "must include at least one letter and one number")
            String password,

            /** Optional, and tidied to +94XXXXXXXXX when given. */
            String phone,
            Role role,
            String province,
            @Size(max = 100) String city,
            @DecimalMin(value = "-90.0", message = "must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "must be between -90 and 90")
            Double latitude,
            @DecimalMin(value = "-180.0", message = "must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "must be between -180 and 180")
            Double longitude,
            @Size(max = 150) String organizationName,
            OrganizationType organizationType,
            CertificationLevel certificationLevel,
            @Size(max = 300) String websiteUrl
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            /** Keeps the session for 30 days instead of 12 hours. */
            Boolean rememberMe
    ) {
        public boolean remember() {
            return Boolean.TRUE.equals(rememberMe);
        }
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password
    ) {
    }

    /** The token is absent for divers and organisations, who wait for administrator verification. */
    public record AuthResponse(
            String token,
            long expiresInSeconds,
            UserDtos.UserResponse user
    ) {
    }
}
