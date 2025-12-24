package com.wind.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * 常用的日期格式化实例
 *
 * @author wuxp
 * @date 2024-04-07 16:09
 **/
@AllArgsConstructor
@Getter
public enum WindDateFormater {

    ISO_8601_EXTENDED_DATETIME(DateTimeFormatter.ofPattern(WindDateFormatPatterns.ISO_8601_EXTENDED_DATETIME)),

    HH_MM_SS(DateTimeFormatter.ofPattern(WindDateFormatPatterns.HH_MM_SS)),

    YYYYMMDD(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYYMMDD)),

    YYYYMMDDHH(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYYMMDDHH)),

    YYYYMMDDHHMM(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYYMMDDHHMM)),

    YYYYMMDDHHMMSS(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYYMMDDHHMMSS)),

    YYYY_MM_DD_HH_MM_SS(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYY_MM_DD_HH_MM_SS)),

    YYYY_MM_DD_HH_MM(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYY_MM_DD_HH_MM)),

    YYYY_MM_DD_HH(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYY_MM_DD_HH)),

    YYYY_MM_DD(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYY_MM_DD)),

    YYYY_MM(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYY_MM)),

    YYYY(DateTimeFormatter.ofPattern(WindDateFormatPatterns.YYYY));

    private final DateTimeFormatter formatter;

    public String format(TemporalAccessor temporal) {
        return formatter.format(temporal);
    }
}
