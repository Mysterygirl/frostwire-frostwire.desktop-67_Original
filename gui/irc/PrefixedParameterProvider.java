/*****************************************************/
/*          This java file is a part of the          */
/*                                                   */
/*           -  Plouf's Java IRC Client  -           */
/*                                                   */
/*   Copyright (C)  2002 - 2004 Philippe Detournay   */
/*                                                   */
/*         All contacts : theplouf@yahoo.com         */
/*                                                   */
/*  PJIRC is free software; you can redistribute     */
/*  it and/or modify it under the terms of the GNU   */
/*  General Public License as published by the       */
/*  Free Software Foundation; version 2 or later of  */
/*  the License.                                     */
/*                                                   */
/*  PJIRC is distributed in the hope that it will    */
/*  be useful, but WITHOUT ANY WARRANTY; without     */
/*  even the implied warranty of MERCHANTABILITY or  */
/*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU   */
/*  General Public License for more details.         */
/*                                                   */
/*  You should have received a copy of the GNU       */
/*  General Public License along with PJIRC; if      */
/*  not, write to the Free Software Foundation,      */
/*  Inc., 59 Temple Place, Suite 330, Boston,        */
/*  MA  02111-1307  USA                              */
/*                                                   */
/*****************************************************/

package irc;

/**
 * Parameter provider with automatic prefix.
 */
public class PrefixedParameterProvider implements ParameterProvider
{
  private String _prefix;
  private ParameterProvider _source;

  /**
   * Create a new PrefixedParameterProvider with the given source and prefix.
   * @param source for fetching parameters.
   * @param prefix to add before all parameter name.
   */
  public PrefixedParameterProvider(ParameterProvider source,String prefix)
  {
    _source=source;
    _prefix=prefix;
  }

  public String getParameter(String name)
  {
    return _source.getParameter(_prefix+name);
  }
}
