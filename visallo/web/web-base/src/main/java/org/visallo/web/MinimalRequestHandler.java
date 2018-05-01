package org.visallo.web;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.v5analytics.webster.App;
import com.v5analytics.webster.RequestResponseHandler;
import org.vertexium.FetchHint;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This is a handler that provides common helper methods, and only depends on {@link Configuration}
 * to be injected.
 */
public abstract class MinimalRequestHandler implements RequestResponseHandler {
    private static final String LOCALE_LANGUAGE_PARAMETER = "localeLanguage";
    private static final String LOCALE_COUNTRY_PARAMETER = "localeCountry";
    private static final String LOCALE_VARIANT_PARAMETER = "localeVariant";

    private final Configuration configuration;

    protected MinimalRequestHandler(Configuration configuration) {
        this.configuration = configuration;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected WebApp getWebApp(HttpServletRequest request) {
        return (WebApp) App.getApp(request);
    }

    protected Locale getLocale(HttpServletRequest request) {
        String language = getOptionalParameter(request, LOCALE_LANGUAGE_PARAMETER);
        String country = getOptionalParameter(request, LOCALE_COUNTRY_PARAMETER);
        String variant = getOptionalParameter(request, LOCALE_VARIANT_PARAMETER);

        if (language != null) {
            return WebApp.getLocal(language, country, variant);
        }
        return request.getLocale();
    }

    protected ResourceBundle getBundle(HttpServletRequest request) {
        WebApp webApp = getWebApp(request);
        Locale locale = getLocale(request);
        return webApp.getBundle(locale);
    }

    protected String getString(HttpServletRequest request, String key) {
        ResourceBundle resourceBundle = getBundle(request);
        return resourceBundle.getString(key);
    }

    /**
     * Attempts to extract the specified parameter from the provided request
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The value of the specified parameter
     * @throws RuntimeException Thrown if the required parameter was not in the request
     */
    protected String getRequiredParameter(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameter(request, parameterName, false);
    }

    protected String[] getOptionalParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameterValues(request, parameterName, true);
    }

    protected String[] getRequiredParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameterValues(request, parameterName, false);
    }

    protected Long getOptionalParameterLong(final HttpServletRequest request, final String parameterName, long defaultValue) {
        Long defaultValueLong = defaultValue;
        return getOptionalParameterLong(request, parameterName, defaultValueLong);
    }

    protected Long getOptionalParameterLong(final HttpServletRequest request, final String parameterName, Long defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Long.parseLong(val);
    }

    protected Integer getOptionalParameterInt(final HttpServletRequest request, final String parameterName, Integer defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(val);
    }

    protected EnumSet<FetchHint> getOptionalParameterFetchHints(HttpServletRequest request, String parameterName, EnumSet<FetchHint> defaultFetchHints) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null) {
            return defaultFetchHints;
        }
        return EnumSet.copyOf(Lists.transform(Arrays.asList(val.split(",")), new Function<String, FetchHint>() {
            @Nullable
            @Override
            public FetchHint apply(String input) {
                return FetchHint.valueOf(input);
            }
        }));
    }

    protected boolean getOptionalParameterBoolean(final HttpServletRequest request, final String parameterName, boolean defaultValue) {
        Boolean defaultValueBoolean = defaultValue;
        return getOptionalParameterBoolean(request, parameterName, defaultValueBoolean);
    }

    protected Boolean getOptionalParameterBoolean(final HttpServletRequest request, final String parameterName, Boolean defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val);
    }

    protected Double getOptionalParameterDouble(final HttpServletRequest request, final String parameterName, Double defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Double.parseDouble(val);
    }

    protected Float getOptionalParameterFloat(final HttpServletRequest request, final String parameterName, Float defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Float.parseFloat(val);
    }

    /**
     * Attempts to extract the specified parameter from the provided request and convert it to a long value
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The long value of the specified parameter
     * @throws RuntimeException Thrown if the required parameter was not in the request
     */
    protected long getRequiredParameterAsLong(final HttpServletRequest request, final String parameterName) {
        return Long.parseLong(getRequiredParameter(request, parameterName));
    }

    /**
     * Attempts to extract the specified parameter from the provided request and convert it to a int value
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The int value of the specified parameter
     * @throws RuntimeException Thrown if the required parameter was not in the request
     */
    protected int getRequiredParameterAsInt(final HttpServletRequest request, final String parameterName) {
        return Integer.parseInt(getRequiredParameter(request, parameterName));
    }

    /**
     * Attempts to extract the specified parameter from the provided request and convert it to a double value
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The double value of the specified parameter
     * @throws RuntimeException Thrown if the required parameter was not in the request
     */
    protected double getRequiredParameterAsDouble(final HttpServletRequest request, final String parameterName) {
        return Double.parseDouble(getRequiredParameter(request, parameterName));
    }

    /**
     * Attempts to extract the specified parameter from the provided request, if available
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The value of the specified parameter if found, null otherwise
     */
    protected String getOptionalParameter(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameter(request, parameterName, true);
    }

    protected String[] getOptionalParameterAsStringArray(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameterValues(request, parameterName, true);
    }

    protected String[] getParameterValues(final HttpServletRequest request, final String parameterName, final boolean optional) {
        String[] paramValues = request.getParameterValues(parameterName);

        if (paramValues == null) {
            Object value = request.getAttribute(parameterName);
            if (value instanceof String[]) {
                paramValues = (String[]) value;
            }
        }

        if (paramValues == null) {
            if (!optional) {
                throw new VisalloException(String.format("Parameter: '%s' is required in the request", parameterName));
            }
            return null;
        }

        return paramValues;
    }

    private String getParameter(final HttpServletRequest request, final String parameterName, final boolean optional) {
        String paramValue = request.getParameter(parameterName);
        if (paramValue == null) {
            Object paramValueObject = request.getAttribute(parameterName);
            if (paramValueObject != null) {
                paramValue = paramValueObject.toString();
            }
            if (paramValue == null) {
                if (!optional) {
                    throw new VisalloException(String.format("Parameter: '%s' is required in the request", parameterName));
                }
                return null;
            }
        }
        return paramValue;
    }

    protected String getAttributeString(final HttpServletRequest request, final String name) {
        String attr = (String) request.getAttribute(name);
        if (attr != null) {
            return attr;
        }
        return getRequiredParameter(request, name);
    }
}
