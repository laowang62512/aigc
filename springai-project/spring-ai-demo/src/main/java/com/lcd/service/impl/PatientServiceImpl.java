package com.lcd.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcd.domain.Patient;
import com.lcd.mapper.PatientMapper;
import com.lcd.service.PatientService;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {
}
