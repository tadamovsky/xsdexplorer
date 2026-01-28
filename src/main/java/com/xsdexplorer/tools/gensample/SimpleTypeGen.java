package com.xsdexplorer.tools.gensample;

import static org.apache.xerces.impl.dv.XSSimpleType.*;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_LENGTH;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MINLENGTH;

import java.util.Random;

import org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

import nl.flotsam.xeger.Xeger;

public class SimpleTypeGen {
    SimpleTypeGen() {

    }

    String genValue(XSSimpleTypeDecl t) {
        
        switch (t.getVariety()) {
        case XSSimpleTypeDefinition.VARIETY_LIST:
        {
            XSSimpleTypeDecl itemType = (XSSimpleTypeDecl) t.getItemType();
            int len = getMinLength(itemType);
            StringList enums = t.getLexicalEnumeration();
            if (enums.isEmpty()) {
                StringBuilder buff = new StringBuilder(genValue(itemType));
                while (--len > 0) {
                    buff.append(" ").append(genValue(itemType));
                }
                return buff.toString();

            } else {
                final int enumsLen = enums.getLength();
                StringBuilder buff = new StringBuilder(enums.item(0));
                for (int i = 1; i<len; ++i) {
                    buff.append(" ").append(enums.item(i % enumsLen));
                }
                return buff.toString();
            }
        }
        case XSSimpleTypeDefinition.VARIETY_UNION:
        {
            XSObjectList members = t.getMemberTypes();
            return genValue((XSSimpleTypeDecl) members.get(0));
        }
        case XSSimpleTypeDefinition.VARIETY_ATOMIC:
        {
            StringList enums = t.getLexicalEnumeration();
            if (!enums.isEmpty()) {
                return enums.item(0);
            }
            
            if (t.getNumeric()) {
                return getNumber(t);
            }
            //System.err.println(t.getName() + ": "+ t.getPrimitiveKind());
            switch (t.getPrimitiveKind()) {
            //date types
            case PRIMITIVE_DURATION:
                return "PT1004199059S";
            case PRIMITIVE_DATETIME:
                try {
                    String val = "2001-10-26T21:32:52+02:00";
                    t.validate(val, null, null); //some disable or force timezone
                    return val;
                } catch (InvalidDatatypeValueException e) {
                    return "2001-10-26T21:32:52";
                }
            case PRIMITIVE_TIME:
                return "21:32:52";
            case PRIMITIVE_DATE:
                return "2001-10-26";
            case PRIMITIVE_GYEARMONTH:
                return "2001-10";
            case PRIMITIVE_GYEAR:
                return "2001";
            case PRIMITIVE_GMONTHDAY:
                return "--05-01";
            case PRIMITIVE_GDAY:
                return "---17";
            case PRIMITIVE_GMONTH:
                return "--05";
            //end of date types
                
            case PRIMITIVE_BOOLEAN:
                try {
                    t.validate("false", null, null); //some force 1/0
                    return "false";
                } catch (InvalidDatatypeValueException e) {
                    return "0";
                }
            case  PRIMITIVE_BASE64BINARY:
                return "++++";
            case PRIMITIVE_HEXBINARY:
                return "3f3c";
            default:
                //TODO ID/IDRef, anyUri?
               StringList pattern = t.getLexicalPattern();
                if (pattern != null && !pattern.isEmpty()) {
                    String p = pattern.item(0);
                    //p = p.replace("\\", "\\\\");
                    //p = p.replaceAll("\\.", "\\\\.");
                    Xeger xeger = new Xeger(p, new Random(0));
                    return xeger.generate();
                }
                return "String";
            }
        }
        default:
            return "String";

        }
    }
    
    private int getMinLength(XSSimpleTypeDefinition t) {
        String len = t.getLexicalFacetValue(FACET_LENGTH);
        if (len != null)
            return Integer.parseInt(len);
        String minLen = t.getLexicalFacetValue(FACET_MINLENGTH);
        if (minLen != null)
            return Integer.parseInt(minLen);
        return 1;
    }
    
    
    private static String getNumber(XSSimpleTypeDefinition simpleType) {
        XSSimpleTypeDecl t = (XSSimpleTypeDecl)simpleType;

        int fractionDigits = -1;
        String str = simpleType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_FRACTIONDIGITS);
        if (str != null) {
            fractionDigits = Integer.parseInt(str);
        }
        
        Double min = getDouble(t.getMinInclusiveValue());
        Object minExclusive = t.getMinExclusiveValue();
        if (minExclusive != null) {
            min = Double.parseDouble(minExclusive.toString()) + 1;
        }

        Double max = getDouble(t.getMaxInclusiveValue());
        Object maxExclusive = t.getMaxExclusiveValue();
        if (maxExclusive != null) {
            max = Double.parseDouble(maxExclusive.toString()) - 1;
        }

        if (min == null && max == null) {
            return getWithFractionDigits(0, fractionDigits);
        } else if (min == null) {
            return getWithFractionDigits(0 <= max ? 0 : max, fractionDigits);
        } else if (max == null) {
            return getWithFractionDigits(0 >= min ? 0 : min, fractionDigits);
        }
        return getWithFractionDigits(0 >= min && 0 <= max ? 0 : (min+max)/2, fractionDigits);
    }

    private static Double getDouble(Object val) {
        return val == null ? null : Double.valueOf(val.toString());
    }
    
    private static String getWithFractionDigits(double d, int fractionDigits) {
        return fractionDigits != -1 ? String.format("%."+fractionDigits+"f", d) : String.valueOf(d);
    }
    
}
