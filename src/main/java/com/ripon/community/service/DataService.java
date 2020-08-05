package com.ripon.community.service;

import java.util.Date;

public interface DataService {
    void recordUV(String ip);
    long calculateUV(Date start, Date end);
    void recordDAU(int userId);
    long calculateDAU(Date start, Date end);
}
