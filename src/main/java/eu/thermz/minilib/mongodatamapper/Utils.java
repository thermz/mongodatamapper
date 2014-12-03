/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.thermz.minilib.mongodatamapper;

import java.util.Date;

/**
 *
 * @author RMuzzi
 */
public class Utils {
	public static Date getUTCDate(final String dateString) {
		return unchecked(() -> getUTCDateFormat().parse(dateString));
	}
}
