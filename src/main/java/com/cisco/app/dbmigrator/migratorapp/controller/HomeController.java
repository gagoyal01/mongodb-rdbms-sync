package com.cisco.app.dbmigrator.migratorapp.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants;

@Controller
public class HomeController {

	@RequestMapping(value="/")
	public ModelAndView test(HttpServletResponse response) throws IOException{
		return new ModelAndView("home");
		
	}
	@RequestMapping(value = "/pingpong.html", method = RequestMethod.HEAD)
	@ResponseBody
	public String getServerStatus() {
		return SyncConstants.SUCCESS;
	}
}
