
package uk.me.chiandh.Sputnik;

/**
 * <p>The <code>NamedObjectException</code> is superclass for any exceptions
 * that may be thrown by the {@link NamedObject NamedObject} class.</p>
 *
 * <p>Copyright: &copy; 2003 Horst Meyerdierks.</p>
 *
 * <p>This programme is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public Licence as
 * published by the Free Software Foundation; either version 2 of
 * the Licence, or (at your option) any later version.</p>
 *
 * <p>This programme is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public Licence for more details.</p>
 *
 * <p>You should have received a copy of the GNU General Public Licence
 * along with this programme; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.</p>
 *
<dl>
<dt><strong>2003-09-17:</strong> hme</dt>
<dd>Initial revision.</dd>
</dl>

@author
  Horst Meyerdierks, http://www.chiandh.me.uk
 */

public class NamedObjectException extends Exception {
  public NamedObjectException()         {super();}
  public NamedObjectException(String s) {super(s);}
}
