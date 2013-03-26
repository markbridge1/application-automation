/*
 * Center for Knowledge Management Kalmanovitz Library, UCSF
 * 
 * The University of California, San Francisco, CA 94143, 415/476-9000 (c) 2012
 * The Regents of the University of California All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.markbridge.application.automation;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 *
 * @author mbridge
 */
public class Window {
    
    public static DateFormat DATE_FORMAT = 
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL, Locale.US);
    
    private RemoteWebDriver driver;
    private String baseUrl;
    private WebDriverBackedSelenium webdriver;
    
    public Window(RemoteWebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        driver.get(baseUrl);
        this.webdriver = new WebDriverBackedSelenium(driver, baseUrl);
    }
    
    public WebElement elem(By by) {
        WebElement element = null;
        try {
            while(true) {
                List<WebElement> elementL = driver.findElements(by);
                if(elementL != null && ! elementL.isEmpty()) {
                    element = elementL.get(0);
            }
                break;
            }
        } catch(Exception ex) {
        }
        return element;
    }
    
//    public WebElement elem(final By by) {
//        //http://seleniumhq.org/docs/04_webdriver_advanced.html
//        WebElement element = (new WebDriverWait(driver, 30L, 100L))
//                .until(new ExpectedCondition<WebElement>() {
//            @Override
//            public WebElement apply(WebDriver input) {
//                return driver.findElement(by);
//            }
//        });
//        
//        return element;
//    }
    
    public WebElement className(String className) {
        return elem(By.className(className));
    }
    
    public WebElement cssSelector(String cssSelector) {
        return elem(By.cssSelector(cssSelector));
    }
    
    public WebElement id(String id) {
        return elem(By.id(id));
    }
    
    public WebElement linkText(String linkText) {
        return elem(By.linkText(linkText));
    }
    
    public WebElement name(String name) {
        return elem(By.name(name));
    }
    
    public WebElement partialLinkText(String partialLinkText) {
        return elem(By.partialLinkText(partialLinkText));
    }
    
    public WebElement tagName(String tagName) {
        return elem(By.tagName(tagName));
    }
    
    public WebElement xpath(String xpathExpression) {
        return elem(By.xpath(xpathExpression));
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }
    
    public RemoteWebDriver getDriver() {
        return driver;
    }
}
