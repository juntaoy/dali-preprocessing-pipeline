<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet  version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"    
    xmlns:func="http://www.text-technology.de/sekimo/functions"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    exclude-result-prefixes="func xs"
    xmlns:saxon="http://saxon.sf.net/"
    extension-element-prefixes="saxon">
        
    <!-- ########## Documentation ##########
        MASXML2SGF.xsl - Transformation of GNOME/MASXML-Annotations to SGF
        Version 16.12.2008, 17:32(CET)
        Project Sekimo (A2) of the DFG Research Group 437
        (c) Daniel Jettka; daniel.jettka@uni-bielefeld.de
        
        Input XML file: file containing inline annotation, validated by XSD
        
        Execution (using XSLT-Processor Saxon9):
        java -jar saxon9.jar [optional Saxon Parameters] -o [XML output filename] [XML input filename] MASXML2SGF.xsl [optional Stylesheet Parameters, see below]
        
        Stylesheet Parameters:
        primary-data (xs:string) - name of a txt-file containing the primary data. If omitted then primary data is generated from the text data of XML input
        virtual-root (xs:string, default: 'body') - name of element which serves as root of the actual annotation to be included in layers ds, mark and rel
        local-xsd (xs:boolean, default: false) - if true then local XSDs are used, false - XSDs from http://www.text-technology.de/sekimo/sgf/... are used 
        meta-root (xs:string, default: 'header') - determines the location of meta data which is to be copied to <sgf:meta>
        info-layer (xs:boolean, default: true) - if true() then an extra layer is built extracting information on author, title and editor from the meta data
        copy-primary-data-to-sgf (xs:boolean, default: true) - determines whether or not primary data should be copied to element <primaryData>
        include-optional-elements (xs:boolean, default: false) - value true includes optional elements into the result of the transformation 
        include-ws-segments (xs:boolean, default: true) - true includes segments for whitespaces into the result of the transformation 
        ####################################
    -->
    
    <xsl:output indent="yes" method="xml" encoding="UTF-8" />
    <xsl:strip-space elements="*"/> 
    
    <!-- ######## Stylesheet Parameters ######## -->
    <xsl:param name="primary-data" as="xs:string?" required="no"/>
    <xsl:param name="virtual-root" select="'body'" as="xs:string" required="no"/>
    <xsl:param name="local-xsd" select="false()" as="xs:boolean" required="no"/>
    <xsl:param name="meta-root" select="'header'" as="xs:string" required="no"/>    
    <xsl:param name="info-layer" select="true()" as="xs:boolean" required="no"/>
    <xsl:param name="copy-primary-data-to-sgf" select="true()" as="xs:boolean" required="no"/>
    <xsl:param name="include-optional-elements" select="false()" as="xs:boolean" required="no"/>
    <xsl:param name="include-ws-segments" select="true()" as="xs:boolean" required="no"/>
    <!-- ################################### -->    
    
    
    <!-- ############### Keys ############### -->
    <xsl:key name="elem-by-id" match="element()" use="@id"/>
    <xsl:key name="elem-by-node-name" match="element()" use="node-name(.)"/>
    <xsl:key name="elem-by-namespace-uri" match="element()" use="namespace-uri(.)"/>
    <xsl:key name="elem-by-namespace-prefix" match="element()" use="prefix-from-QName(node-name(.))"/>
    <xsl:key name="ante-by-mark-id" match="ante" use="@current|anchor/@antecedent"/>
    <!-- ################################### -->    
     
    
    <!--########### Global Variables ###########-->   
    <!--Variable containing the element which is provided by stylesheet parameter virtual-root-->
    <xsl:variable name="virtual-root-node">
        <xsl:call-template name="return-node-or-root">
            <xsl:with-param name="node-name" select="$virtual-root" as="xs:string"/>
        </xsl:call-template>
    </xsl:variable>
    
    <!--Variable containing the element which is provided by stylesheet parameter meta-root-->
    <xsl:variable name="meta-root-node">
        <xsl:call-template name="return-node-or-root">
            <xsl:with-param name="node-name" select="$meta-root" as="xs:string"/>
        </xsl:call-template>
    </xsl:variable>
    
    <!--name of the input xml file, e.g. used for xml:id in <sgf:corpusData>-->
    <xsl:variable name="baseFileName" select="substring-before(tokenize(document-uri(/), '/')[last()], '.xml')" as="xs:string"/>
    
    <!--defines the directory of the schema location with respect to the stylesheet parameter $local-xsd-->
    <xsl:variable name="xsd-location" select="if($local-xsd) then '' else 'http://coli.lili.uni-bielefeld.de/Texttechnologie/Forschergruppe/sekimo/sgf/'" as="xs:string"/>
        
    <!--Layers to be included in the SGF annotation (stating namespace-uri, prefix, schemaLocation and included elements for each layer)-->
    <xsl:variable name="layers" as="element()+">
        <xsl:message select="' $ Instantiating variable $layers'"/>
        <layer namespace-uri="http://www.text-technology.de/anawiki/masxml" prefix="masxml" schema="{concat($xsd-location, 'ana_masxml.xsd')}">
            <xsl:for-each select="distinct-values($virtual-root-node//element()/local-name())[.!='layspecial']">
                <xsl:attribute name="{.}" select="1"/>
            </xsl:for-each>
        </layer>
        <layer ne="1" namespace-uri="http://www.text-technology.de/anawiki/mark" prefix="mark" schema="{concat($xsd-location, 'ana_markables.xsd')}"/>
        <layer s="1" p="1" namespace-uri="http://www.text-technology.de/anawiki/ds" prefix="ds" schema="{concat($xsd-location, 'ana_doc.xsd')}"/>
        <layer ante="1" namespace-uri="http://www.text-technology.de/anawiki/relation" prefix="rel" schema="{concat($xsd-location, 'ana_semrels.xsd')}"/>
        <xsl:message select="'   - instantiated $layers'"/>
    </xsl:variable>
    
    <!--Saving primary data in variable (inferred either by stylesheet parameter and provided txt-file or on the basis of the textual data from XML input)-->
    <xsl:variable name="primary-data-string" as="xs:string">
        <xsl:message select="' $ Instantiating variable $primary-data-string'"/>
        <xsl:choose>
            <!--when there is a txt-file containing primary data-->
            <xsl:when test="$primary-data">
                <xsl:message select="concat('   - got primary-data from ', $primary-data)"/>
                <xsl:copy-of select="unparsed-text($primary-data, 'ISO-8859-1')"/>
            </xsl:when>
            <!--when there is no txt-file provided-->
            <xsl:otherwise>
                <xsl:for-each select="$virtual-root-node">                    
                    <!--textual data from XML being inferred-->
                    <xsl:call-template name="build-primary-data-from-XML">
                        <xsl:with-param name="XMLdata" as="node()+">
                            <!--XML data getting normalized-->
                            <xsl:call-template name="normalize"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:message select="'   - instantiated $primary-data-string.'"/>
    </xsl:variable>
    
    <!--tokenized primary data-->
    <xsl:variable name="tokenizedPrimaryData_WS" select="tokenize($primary-data-string, '\S+')[. != '']" as="xs:string*"/>
    
    <!--primary data without whitespaces--> 
    <xsl:variable name="primaryData_noWS" select="replace($primary-data-string, '\s', '')" as="xs:string?"/>
    
    <!--textual content of the element determined by stylesheet parameter virtual-root (without whitespaces)-->
    <xsl:variable name="textData_noWS" select="replace(string-join($virtual-root-node//text(), ''), '\s', '')" as="xs:string?"/>
        
    <!--every single char of the primary data is saved in an empty element also containing the position of the char-->
    <xsl:variable name="markedPrimaryData" as="element()+">
        <xsl:call-template name="markPrimaryData">
            <xsl:with-param name="text" select="$primary-data-string"/>
            <xsl:with-param name="posCounter" select="0"/>
        </xsl:call-template>
    </xsl:variable>
    
    <!--Variable contains segments which were built with respect to elements from the XML input-->
    <xsl:variable name="allSegments" as="element()+">
        <xsl:message select="' $ Instantiating variable $allSegments'"/>
        <!-- Saving segments for further processing -->
        <xsl:variable name="tempSEQ" as="element()+">
            <!--if to be included, whitespace segements are built-->
            <xsl:if test="$include-ws-segments">
                <xsl:message select="'   - Building whitespace segments'"/>         
                <xsl:call-template name="wsSegments">
                    <xsl:with-param name="charPos" select="0" as="xs:integer"/>
                </xsl:call-template> 
            </xsl:if>
            <!--segments for elements to be regarded are built-->
            <xsl:message select="'   - Building element segments'"/>         
            <xsl:for-each select="$virtual-root-node//*[index-of( (for $nameAtt in $layers//@*[name()!='namespace-uri' and name()!='prefix'] return local-name($nameAtt)), local-name(.) ) &gt; 0]">
                    <xsl:call-template name="SEG4elements"/>
            </xsl:for-each>
        </xsl:variable>
        <!--double segments are deleted and segments get sorted-->
        <xsl:variable name="tempSEQ2" select="func:sort-segments(func:delete-multiple-segments($tempSEQ))" as="element(segment)+"/>
        <!--segments get an xml:id and returned to save them in the variable-->
        <xsl:for-each select="$tempSEQ2">
            <segment xml:id="{concat('seg', position())}">
                <xsl:copy-of select="@*"/>                
            </segment>
        </xsl:for-each>
        <xsl:message select="'   - instantiated $allSegments'"/>
    </xsl:variable>
    
    <!--in this variable the elements, namespace-uri and prefix for the different layers are saved -->
    <xsl:variable name="layerSEG" as="element()+">
        <xsl:message select="' $ Instantiating variable $layerSEG'"/>
        <xsl:for-each select="$layers">
            <xsl:variable name="thisLayersElemNames" select="for $att in @*[name(.)!='prefix' and name(.)!='namespace-uri'] return name($att)" as="xs:string+"/>
            <xsl:variable name="thisLayersPrefix" select="@prefix" as="xs:string?"/>
            <xsl:variable name="thisLayersURI" select="@namespace-uri" as="xs:anyURI?"/>
            <xsl:message select="'   - Processing layer: ', $thisLayersPrefix, '/', $thisLayersURI"/>
            <layer prefix="{$thisLayersPrefix}" namespace-uri="{$thisLayersURI}">
                <xsl:for-each select="$virtual-root-node">
                    <xsl:call-template name="copy-nodes-by-name" exclude-result-prefixes="#all">
                        <xsl:with-param name="elemNames" select="$thisLayersElemNames" as="xs:string+"/>
                        <xsl:with-param name="prefix" select="$thisLayersPrefix" as="xs:string"/>
                    </xsl:call-template>
                </xsl:for-each>                
            </layer>
        </xsl:for-each>        
        <xsl:message select="'   - instantiated $layerSEG'"/>
    </xsl:variable>    
    <!-- ################################### -->    
    
    
    <!-- ############# Templates ############ -->    
    <xsl:template match="/" as="element()">
        <xsl:copy-of select="func:param-messages(), func:check-primary-data()"/>
        <sgf:corpusData xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="{concat('http://www.text-technology.de/sekimo', ' ', $xsd-location, 'sgf.xsd')}"
            xmlns="http://www.text-technology.de/sekimo"
            xmlns:sgf="http://www.text-technology.de/sekimo"
            sgfVersion="1.0" type="text" xml:id="{func:return-ID($baseFileName)}">            
            <sgf:meta>
                <xsl:for-each select="$meta-root-node">
                    <xsl:call-template name="copy-nodes-with-prefix">
                        <xsl:with-param name="prefix" select="prefix-from-QName(node-name(.))" as="xs:string?"/>
                        <xsl:with-param name="uri" select="(namespace-uri(), xs:anyURI('http://www.text-technology.de/anawiki/meta'))[.!=''][1]" as="xs:anyURI"/>
                    </xsl:call-template>
                </xsl:for-each>
            </sgf:meta>
            <sgf:primaryData start="0" end="{string-length($primary-data-string)}">
                <xsl:if test="exists(//(@xml:lang|@lang))">
                    <xsl:attribute name="xml:lang" select="(//@*[local-name()='lang'])[1]"/>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="$copy-primary-data-to-sgf">
                        <textualContent>
                            <xsl:copy-of select="$primary-data-string"/>
                        </textualContent>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="$primary-data">
                                <location uri="{$primary-data}"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <textualContent>
                                    <xsl:copy-of select="$primary-data-string"/>
                                </textualContent>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="$include-optional-elements">
                    <sgf:checksum algorithm="md5"/>
                </xsl:if>  
            </sgf:primaryData>
            <sgf:segments>
                <xsl:for-each select="$allSegments">
                    <sgf:segment>
                        <xsl:copy-of select="@*"/>
                    </sgf:segment> 
                </xsl:for-each>
            </sgf:segments>
            <xsl:if test="$info-layer and (exists($meta-root-node/(descendant::*[local-name()='title']|descendant::*[local-name()='editor']|descendant::*[local-name()='author'])))">
                <xsl:copy-of select="func:return-info-layer()"/>
            </xsl:if>
            <xsl:for-each select="$layers">
                <xsl:variable name="thisLayersSchemaLocation" select="@schema" as="xs:anyURI"/>
                <xsl:variable name="thisLayersURI" select="@namespace-uri" as="xs:anyURI?"/>
                <xsl:variable name="thisLayersPrefix" select="@prefix" as="xs:string?"/>
                <sgf:annotation xml:id="{concat(func:return-ID($baseFileName), '_', generate-id())}">        
                    <sgf:level xml:id="{concat(func:return-ID($baseFileName), '_', $thisLayersPrefix)}">
                        <xsl:if test="$thisLayersPrefix='mark'">
                            <xsl:attribute name="priority" select="1"/>
                        </xsl:if>
                        <xsl:if test="$include-optional-elements">
                            <sgf:meta>
                                <olac:olac xmlns:olac="http://www.language-archives.org/OLAC/1.0/"
                                    xmlns="http://purl.org/dc/elements/1.1/"
                                    xmlns:dcterms="http://purl.org/dc/terms/"
                                    xsi:schemaLocation="http://www.language-archives.org/OLAC/1.0/ meta/olac.xsd">
                                    <format xmlns="http://purl.org/dc/elements/1.1/"/>
                                    <dcterms:isFormatOf/>
                                    <creator/>
                                    <date/>
                                    <description/>
                                </olac:olac>
                            </sgf:meta>
                        </xsl:if>
                        <sgf:layer>
                            <xsl:if test="$thisLayersURI != ''">
                                <xsl:namespace name="{$thisLayersPrefix}" select="$thisLayersURI"/>                                    
                            </xsl:if>
                            <!--<xsl:if test="$thisLayersPrefix = ('ds', 'mark', 'rel')">-->
                                <xsl:attribute name="xsi:schemaLocation" select="concat(concat($thisLayersURI, ' ')[$thisLayersURI!=''], $thisLayersSchemaLocation)"/>
                            <!--</xsl:if>-->
                            <xsl:for-each select="$layerSEG[@namespace-uri=$thisLayersURI]/*">
                                <xsl:call-template name="copy-nodes-with-prefix">
                                    <xsl:with-param name="prefix" select="$thisLayersPrefix" as="xs:string?"/>
                                    <xsl:with-param name="uri" select="$thisLayersURI" as="xs:anyURI?"/>
                                </xsl:call-template>
                            </xsl:for-each>               
                        </sgf:layer>
                    </sgf:level>   
                </sgf:annotation>
            </xsl:for-each>
            <xsl:if test="$include-optional-elements">
                <sgf:log></sgf:log>
            </xsl:if>
        </sgf:corpusData>      
    </xsl:template>
    
    <!-- the provided string $text is saved charwise into empty elements containing information about the char and its position in $text -->
    <xsl:template name="markPrimaryData" as="element(char)+" exclude-result-prefixes="#all">
        <xsl:param name="text" as="xs:string"/>
        <xsl:param name="posCounter" as="xs:integer"/>
        <xsl:choose>
            <xsl:when test="string-length($text) &gt; 0">                
                <xsl:if test="not(matches(substring($text,1,1), '\s'))">                
                    <char pos="{$posCounter}" value="{substring($text,1,1)}"/>            
                </xsl:if>
                <xsl:call-template name="markPrimaryData">
                    <xsl:with-param name="posCounter" select="$posCounter + 1"/>
                    <xsl:with-param name="text" select="substring($text, 2, string-length($text) - 1)"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>                
                <char pos="{$posCounter}" value=""/>       
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- a segment is to be built for the context element/reference to the primary data is realized by start and end position attributes -->
    <xsl:template name="SEG4elements" as="element(segment)?">
        <xsl:variable name="precedingText" select="replace(string-join((preceding::element() intersect $virtual-root-node//element())/text(), ''), '\s', '')" as="xs:string"/>
        <xsl:variable name="descendantText" select="replace(string-join(descendant::text(), ''), '\s', '')" as="xs:string"/>
        <xsl:variable name="positionAtts" select="func:returnPositionAtts($precedingText, $descendantText, $markedPrimaryData)" as="attribute()+"/>
        <xsl:if test="(number($positionAtts[name()='start']) &lt;= number($positionAtts[name()='end'])) and string-length($precedingText) &lt;= string-length($primaryData_noWS)">
            <segment type="char">
                <xsl:copy-of select="$positionAtts"/>
            </segment>
        </xsl:if>  
    </xsl:template>    
            
    <!-- this template builds segments for whitespace  -->
    <xsl:template name="wsSegments" as="element(segment)*">
        <xsl:param name="charPos" as="xs:integer"/>
        <xsl:if test="($charPos+1) &lt; string-length($primary-data-string)">
            <xsl:if test="matches(substring($primary-data-string, $charPos+1, 1), '\s')">
                <segment charref="{string-to-codepoints(substring($primary-data-string, $charPos+1, 1))}" type="ws" start="{$charPos}" end="{$charPos+1}"/>
            </xsl:if>            
            <xsl:call-template name="wsSegments">
                <xsl:with-param name="charPos" select="$charPos + 1" as="xs:integer"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <!-- here a recursive copy of the context element and its descendants is performed. Only elements in the provided namespace
        $namespace get copied -->
    <xsl:template name="copy-nodes-by-name" as="element()*">
        <xsl:param name="elemNames" as="xs:string+"/>
        <xsl:param name="prefix" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="( index-of($elemNames, name()) &gt; 0)
                or
                (if($prefix='mark' and not(self::*[local-name()='ne'])) then exists(key('ante-by-mark-id', @id, $virtual-root-node)) else false() )">
                <xsl:element name="{func:mapElemName(local-name(), $prefix)}">
                    <xsl:variable name="precedingText" select="replace(string-join((preceding::element() intersect $virtual-root-node//element())/text(), ''), '\s', '')" as="xs:string"/>
                    <xsl:variable name="descendantText" select="replace(string-join(descendant::text(), ''), '\s', '')" as="xs:string"/>
                    <xsl:variable name="positionAtts" as="attribute()+">
                        <xsl:copy-of select="func:returnPositionAtts($precedingText, $descendantText, $markedPrimaryData)"/>
                    </xsl:variable>
                    <xsl:variable name="segmentID" select="$allSegments[@start=$positionAtts[name()='start'] and @end=$positionAtts[name()='end']]/@xml:id" as="xs:string*"/>
                    <xsl:if test="$segmentID">
                        <xsl:attribute name="sgf:segment" select="$segmentID" namespace="http://www.text-technology.de/sekimo"/>
                    </xsl:if>               
                    <xsl:attribute name="xml:id">
                        <xsl:choose>
                            <xsl:when test="exists(@*[local-name()='id'])">
                                <xsl:value-of select="concat(concat($prefix, '_')[$prefix!='masxml'], @*[local-name()='id'])"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:variable name="position">
                                    <xsl:number select="." level="any"/>
                                </xsl:variable>
                                <xsl:attribute name="xml:id" select="concat(concat($prefix, '_')[$prefix!='masxml'], local-name(), $position)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="$prefix='masxml'">
                            <xsl:copy-of select="@* except @id"/>
                        </xsl:when>
                        <xsl:when test="$prefix='mark'">
                            <xsl:for-each select="(@gen, @AAgen)[1], (@num, @AAnum)[1], (@per, @AAper)[1], (@cat, @AAcat)[1]">
                                <xsl:copy-of select="func:mapAtt2ARRAU(.)"/>
                            </xsl:for-each>
                            <xsl:attribute name="reference" select="'unmarked'"/>
                            <xsl:if test="local-name()='ne'">
                                <xsl:copy-of select="func:mapAtt2ARRAU(((*[local-name()='nphead']/*[local-name()='W'])/@Lpos)[1])"/>
                            </xsl:if>
                            <xsl:if test="local-name()=('s', 'unit')">
                                <xsl:attribute name="type" select="'prop'"/>
                            </xsl:if>
                        </xsl:when>
                        <xsl:when test="$prefix='rel'">
                            <xsl:copy-of select="func:mapRelTypeAttr(@rel)"/>
                            <xsl:attribute name="phorIDRef" select="concat('mark_', @current)"/>
                            <xsl:attribute name="antecedentIDRefs" select="string-join(for $ante in *[local-name()='anchor']/@antecedent return concat('mark_', $ante), ' ')"/>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:for-each select="*">
                        <xsl:call-template name="copy-nodes-by-name">
                            <xsl:with-param name="elemNames" select="$elemNames" as="xs:string+"/>
                            <xsl:with-param name="prefix" select="$prefix" as="xs:string"/>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="*">
                    <xsl:call-template name="copy-nodes-by-name">
                        <xsl:with-param name="elemNames" select="$elemNames" as="xs:string+"/>
                        <xsl:with-param name="prefix" select="$prefix" as="xs:string"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>    
        
    <!--recursive copy of elementen, $prefix is realized as the prefix of the elements-->
    <xsl:template name="copy-nodes-with-prefix">
        <xsl:param name="prefix" as="xs:string?"/>
        <xsl:param name="uri" as="xs:anyURI?"/>
        <xsl:choose>
            <xsl:when test="self::text()|self::attribute()">
                <xsl:copy-of select="."/>
            </xsl:when>
            <xsl:when test="self::element()">
                <xsl:element name="{if($prefix) then concat($prefix, ':', local-name()) else node-name(.)}" namespace="{$uri}">
                    <xsl:for-each select="element()|attribute()|text()">
                        <xsl:call-template name="copy-nodes-with-prefix">
                            <xsl:with-param name="prefix" select="$prefix" as="xs:string?"/>
                            <xsl:with-param name="uri" select="$uri" as="xs:anyURI"/>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <!--this case refers to the document root-->
                <xsl:for-each select="attribute()|comment()|text()|processing-instruction()|element()">
                    <xsl:call-template name="copy-nodes-with-prefix">
                        <xsl:with-param name="prefix" select="$prefix" as="xs:string?"/>
                        <xsl:with-param name="uri" select="$uri" as="xs:anyURI"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!--normalizes XML data, derivate of SVN/software/tools/trunk/normalize2.xsl
        "Angepasste und korrigierte Version des (Ungültigen) Originals von http://dpawson.co.uk/xsl/sect2/N8321.html#d12364e18" -->        
    <xsl:template name="normalize">
        <xsl:choose>
            <xsl:when test="self::element()">
                <xsl:copy>
                    <xsl:for-each select="attribute()|comment()|text()|processing-instruction()|element()">
                        <xsl:call-template name="normalize"/>
                    </xsl:for-each>
                </xsl:copy>
            </xsl:when>
            <xsl:when test="self::attribute()">
                <xsl:choose>
                    <xsl:when test="not( prefix-from-QName(node-name(.)) )">
                        <xsl:attribute name="{local-name(.)}" select="if(. instance of xs:string) then normalize-space(.) else ."/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="{name(.)}" namespace="{namespace-uri(.)}" select="if(. instance of xs:string) then normalize-space(.) else ."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="self::comment()">
                <xsl:comment select="normalize-space(.)"/>
            </xsl:when>
            <xsl:when test="self::text()">
                <xsl:value-of select="normalize-space(.)"/>
            </xsl:when>
            <xsl:when test="self::processing-instruction()">                            
                <xsl:processing-instruction name="{local-name(.)}" select="normalize-space(.)"/>
            </xsl:when>
            <xsl:otherwise>
                <!--this case refers to the document root-->
                <xsl:for-each select="attribute()|comment()|text()|processing-instruction()|element()">
                    <xsl:call-template name="normalize"/>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!--this template generates a text on the basis of the textual content of the provided $XMLdata.
        The text serves as primary data and therefore as a reference of the start and end position of elements--> 
    <xsl:template name="build-primary-data-from-XML" as="xs:string">
        <xsl:param name="XMLdata" as="node()+"/>
        <!--saving extracted textual content from XML data in $textData-->        
        <xsl:variable name="textData" as="xs:string*">
            <xsl:for-each select="$XMLdata//(text()|*[local-name()='p'])">
                <xsl:choose>
                    <!--returning text nodes unchanged-->
                    <xsl:when test="self::text()">
                        <xsl:value-of select="."/>
                    </xsl:when>
                    <!--inserting line break for <p>-elements (paragraphs)-->
                    <xsl:otherwise>
                        <xsl:if test="position()!=1">
<xsl:text>
</xsl:text>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:variable>        
        <xsl:if test="empty($textData)">
            <xsl:message select="' *** ATTENTION: there is no textual content in XML input to generate primary data!'"/>
        </xsl:if>
        <!--processing sequence of strings-->
        <xsl:variable name="tempTextData">
            <xsl:for-each select="$textData">
                <xsl:variable name="pos" select="position()" as="xs:integer"/>
                <!--conditions for returning a whitespace before a string;
                    string-to-codepoints for " and '-->
                <xsl:if test="$pos!=1">
                    <xsl:variable name="stringBefore" select="$textData[$pos - 1]" as="xs:string"/>
                    <!--conditions for returning a whitespace before the current string-->
                    <xsl:choose>
                        <xsl:when test="string-length(.)=1">
                            <!--conditions to be fulfilled to return a blank-->
                            <xsl:if test="
                                (:###vor Satz- und Sonderzeichen soll kein Leerzeichen ausgegeben werden###:)
                                not(matches(., '[.,;:!\?%)\]*´`°\|}]')) and
                                (:###auch nicht vor Zeichen, denen eine öffnende Klammer vorausgeht###:)
                                not(matches($stringBefore, '[(\[{]')) and
                                (:###falls der aktuelle und der vorhergehende String ein Klitikon bilden wird kein Leerzeichen ausgegeben###:)
                                not(func:twoStringsFormClitic($stringBefore, .)) and
                                (:###falls der aktuelle String ein - ist, wird zwischen versch. Fällen unterschieden, ob ein Leerzeichen ausgegeben wird###:)
                                (if(.='-' and matches($textData[$pos+1], ',|und|oder|and|or')) then false() else true()) and
                                (:###falls der aktuelle String ein ' ist und das vorhergehende oder nachfolgende Zeichen ein s (Genitiv), wird kein Leerzeichen ausgegeben###:)
                                (if(deep-equal(string-to-codepoints(.), 39) and ($textData[$pos + 1]='s' or substring($stringBefore, string-length($stringBefore))='s')) then false() else true()) and
                                (:###falls der aktuelle String ein ' oder &quot; ist, und es gemessen an allen vorhergehenden gleichen Zeichen an gerader Position steht, wird davor kein Leerzeichen ausgegeben###:)
                                (if(deep-equal(string-to-codepoints(.), 39) or deep-equal(string-to-codepoints(.), 34)) then ((floor(count($textData[position() &lt; $pos][.=current()]) div 2)) = (count($textData[position() &lt; $pos][.=current()]) div 2))  else true()) and                                
                                (:###falls der vorhergehende String ein doppeltes ' ('') ist und er gemessen an allen vorhergehenden gleichen Strings an ungerader Position steht, wird vor dem aktuellen Zeichen kein Leerzeichen ausgegeben###:)
                                (if(deep-equal(string-to-codepoints($stringBefore), (39, 39))) then ((floor(count($textData[position() &lt; ($pos - 1)][.=$stringBefore]) div 2)) = (count($textData[position() &lt;  ($pos - 1)][.=$stringBefore]) div 2))  else true()) and
                                (:###falls es sich beim aktuellen Zeichen um € oder $ handelt und davor eine Zahl steht, wird kein Leerzeichen ausgegeben###:)
                                (if(matches(substring($stringBefore, string-length($stringBefore)), '[0-9]')) then not(matches(current(), '[$€]')) else true()) and
                                (:###falls es sich beim aktuellen Zeichen um eine Zahl handelt und davor ein $ oder € steht, wird kein Leerzeichen ausgegeben###:)
                                (if(matches(substring(., string-length(.)), '[0-9]')) then not(matches($stringBefore, '[$€]')) else true()) and
                                (:###falls es sich beim vorhergehenden oder aktuellen Zeichen um einen Zeilenumbruch handelt, wird kein Leerzeichen ausgegeben###:)
                                not(deep-equal(string-to-codepoints($stringBefore), 10) or deep-equal(string-to-codepoints(.), 10))">
                                <!--returning blank-->
                                <xsl:value-of select="' '"/>
                            </xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:choose>
                                <xsl:when test="string-length($stringBefore) = 1">
                                    <!--conditions have to be fulfilled to return a blank-->
                                    <xsl:if test="
                                        (:###Vor Zeichen, denen eine öffnende Klammer vorausgeht, soll kein Leerzeichen ausgegeben werden###:)
                                        not(matches($stringBefore, '[(\[{]')) and
                                        (:###falls der vorhergehende String ein ' ist und das aktuelle Zeichen ein s (Genitiv), wird kein Leerzeichen ausgegeben###:)
                                        (if(deep-equal(string-to-codepoints($stringBefore), 39) and .='s') then false() else true()) and
                                        (:###falls der vorhergehende String ein ' oder &quot; ist, und es gemessen an allen vorhergehenden gleichen Zeichen an gerader Position steht, wird vor dem aktuellen String kein Leerzeichen ausgegeben###:)
                                        (if(deep-equal(string-to-codepoints($stringBefore), 39) or deep-equal(string-to-codepoints($stringBefore), 34)) then ((floor(count($textData[position() &lt; $pos][.=$stringBefore]) div 2)) = (count($textData[position() &lt; $pos][.=$stringBefore]) div 2))  else true()) and
                                        (:###falls der aktuelle String ein doppeltes ' ('') ist und er gemessen an allen vorhergehenden gleichen Strings an gerader Position steht, wird davor kein Leerzeichen ausgegeben###:)
                                        (if(deep-equal(string-to-codepoints(.), (39, 39)))  then ((floor(count($textData[position() &lt; $pos][.=current()]) div 2)) = (count($textData[position() &lt; $pos][.=current()]) div 2))  else true()) and
                                        (:###falls es sich beim aktuellen Zeichen eine Zahl handelt und davor ein $ oder € steht, wird kein Leerzeichen ausgegeben###:)
                                        (if(matches(substring(., string-length(.)), '[0-9]')) then not(matches($stringBefore, '[$€]')) else true()) and
                                        (:###falls es sich beim vorhergehenden oder aktuellen Zeichen um einen Zeilenumbruch handelt, wird kein Leerzeichen ausgegeben###:)
                                        not(deep-equal(string-to-codepoints($stringBefore), 10) or deep-equal(string-to-codepoints(.), 10))">
                                        <!--returning blank-->
                                        <xsl:value-of select="' '"/>
                                    </xsl:if>
                                </xsl:when>
                                <xsl:otherwise>
                                    <!--conditions have to be fulfilled to return a blank-->
                                    <xsl:if test="
                                        (:### aktueller String sollte nicht 's sein ###:)
                                        not((string-length(.) = 2) and (deep-equal(string-to-codepoints(substring(., 1, 1)), 39) and substring(., 2, 1)='s')) and
                                        (:###falls der aktuelle String ein doppeltes ' ('') ist und er gemessen an allen vorhergehenden gleichen Strings an gerader Position steht, wird davor kein Leerzeichen ausgegeben###:)
                                        (if(deep-equal(string-to-codepoints(.), (39, 39)))  then ((floor(count($textData[position() &lt; $pos][.=current()]) div 2)) = (count($textData[position() &lt; $pos][.=current()]) div 2))  else true()) and
                                        (:###falls der vorhergehende String ein doppeltes ' ('') ist und er gemessen an allen vorhergehenden gleichen Strings an ungerader Position steht, wird vor dem aktuellen Zeichen kein Leerzeichen ausgegeben###:)
                                        (if(deep-equal(string-to-codepoints($stringBefore), (39, 39))) then ((floor(count($textData[position() &lt; ($pos - 1)][.=$stringBefore]) div 2)) != (count($textData[position() &lt;  ($pos - 1)][.=$stringBefore]) div 2))  else true()) and
                                        (:###falls es sich beim vorhergehenden oder aktuellen Zeichen um einen Zeilenumbruch handelt, wird kein Leerzeichen ausgegeben###:)
                                        not(deep-equal(string-to-codepoints($stringBefore), 10) or deep-equal(string-to-codepoints(.), 10))">
                                        <!--returning blank-->
                                        <xsl:value-of select="' '"/>
                                    </xsl:if>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <!--returning current string itself-->
                <xsl:value-of select="."/>
            </xsl:for-each>
        </xsl:variable>
        <xsl:value-of select="string-join($tempTextData, '')"/>
    </xsl:template>
    
    
    <!--template identifies a node by its name. If that is impossible either the document-node or an error is returned -->
    <xsl:template name="return-node-or-root">
        <xsl:param name="node-name" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$node-name!=''">
                <xsl:choose>
                    <xsl:when test="count(/descendant::*[name()=$node-name]) = 1">
                        <xsl:copy-of select="/descendant::*[name()=$node-name]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:message select="concat(' *** FATAL ERROR: $node-name ', $node-name, ' does not identify unique node.')" terminate="yes"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="/"/>
            </xsl:otherwise>
        </xsl:choose>        
    </xsl:template>
    <!-- ######################################### -->
    
    
    <!-- ################ Functions ################ -->    
    
    <!-- generating layer 'info' containing information about author, editor, title -->
    <xsl:function name="func:return-info-layer" as="element()">
        <xsl:for-each select="$meta-root-node">
            <sgf:annotation xml:id="{concat(func:return-ID($baseFileName), '_', generate-id())}" xmlns:sgf="http://www.text-technology.de/sekimo">        
                <sgf:level xml:id="{concat(func:return-ID($baseFileName), '_info')}">
                    <sgf:layer xsi:schemaLocation="{concat('http://www.text-technology.de/anawiki/info ', $xsd-location, 'ana_info.xsd')}">
                        <xsl:namespace name="info" select="'http://www.text-technology.de/anawiki/info'"/>        
                        <xsl:for-each select="(descendant::*[local-name()='title']|descendant::*[local-name()='editor']|descendant::*[local-name()='author'])">
                            <xsl:element name="{concat('info:', local-name())}" namespace="http://www.text-technology.de/anawiki/info">
                                <xsl:value-of select="."/>
                            </xsl:element>
                        </xsl:for-each>      
                    </sgf:layer>
                </sgf:level>   
            </sgf:annotation>
        </xsl:for-each>
    </xsl:function>
    
    <xsl:function name="func:sort-segments" as="element(segment)+">
        <xsl:param name="segments" as="element(segment)+"/>        
        <xsl:message select="'   - Sorting segments'"/>           
        <xsl:for-each select="$segments">
            <xsl:sort select="@start" data-type="number"/>            
            <xsl:sort select="@end" data-type="number" order="descending"/>
            <xsl:copy-of select="."/>
        </xsl:for-each>
    </xsl:function>
    
    <xsl:function name="func:delete-multiple-segments" as="element(segment)+">        
        <xsl:param name="segments" as="element(segment)+"/>
        <xsl:message select="'   - Deleting multiple segments'"/>
        <xsl:for-each select="$segments">
            <xsl:variable name="startpos" select="@start" as="xs:integer"/>
            <xsl:variable name="endpos" select="@end" as="xs:integer"/>
            <xsl:variable name="position" select="position()" as="xs:integer"/>
            <xsl:if test="not(exists($segments[position() &gt; $position and @start=$startpos and @end=$endpos]))">
                <xsl:copy-of select="."/>
            </xsl:if>                
        </xsl:for-each>
    </xsl:function>
    
    <!-- Function generates attributes, which contain the start and end positions of a string, as reference the preceding and following text is used-->
    <xsl:function name="func:returnPositionAtts" as="attribute()+">
        <xsl:param name="precedingText" as="xs:string"/>    
        <xsl:param name="descendantText" as="xs:string"/>    
        <xsl:param name="charsPrimaryData" as="element()+"/>
        <xsl:attribute name="start" select="$charsPrimaryData[position() = (string-length($precedingText) + 1)]/@pos"/>
        <!-- Special case: last segment is one for an empty element-->
        <xsl:attribute name="end" select="$charsPrimaryData[position() = (string-length($precedingText) + (string-length($descendantText)[.!=0], 1)[1])]/@pos + (if(string-length($descendantText)&gt;0) then 1 else 0)"/>
    </xsl:function>
    
  
    <!--Function tests whether two strings ($string1 und $string2) form a clitic-->
    <xsl:function name="func:twoStringsFormClitic" as="xs:boolean">
        <xsl:param name="string1" as="xs:string"/>
        <xsl:param name="string2" as="xs:string"/>
        <!--see Nübling, Damaris (1992): Klitika im Deutschen: Schriftsprache, Umgangssprache, alemannische Dialekte. Gunter Narr Verlag, S. 160-->
        <xsl:choose>
            <xsl:when test="matches($string1, '[Bb]ei|[Ii]|[Aa]|[Vv]o')[$string2='m']">
                <xsl:copy-of select="true()"/>
            </xsl:when>
            <xsl:when test="matches($string1, '[Zz]u')[matches($string2, '[mr]')]">
                <xsl:copy-of select="true()"/>
            </xsl:when>
            <xsl:when test="matches($string1, '[Aa]n|[Ii]n|[Nn]eben|[Uu]m|[Dd]urch')[$string2='s']">
                <xsl:copy-of select="true()"/>
            </xsl:when>
            <xsl:when test="matches($string1, '[Aa]uf|[Vv]or')[matches($string2, '[ms]')]">
                <xsl:copy-of select="true()"/>
            </xsl:when>
            <xsl:when test="matches($string1, '[Ff]ür')[matches($string2, '[ns]')]">
                <xsl:copy-of select="true()"/>
            </xsl:when>
            <xsl:when test="matches($string1, '[Üü]ber|[Uu]nter|[Hh]inter')[matches($string2, '[mns]')]">
                <xsl:copy-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>     
    </xsl:function>
            
    
    <xsl:function name="func:mapElemName" as="xs:string">
        <xsl:param name="elemName" as="xs:string"/>
        <xsl:param name="prefix" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$prefix='mark'">
                <xsl:value-of select="'markable'"/>
            </xsl:when>
            <xsl:when test="$prefix='ds'">
                <xsl:value-of select="('sentence'[$elemName='s'], 'paragraph'[$elemName='p'])[1]"/>
            </xsl:when>
            <xsl:when test="$prefix='rel'">
                <xsl:value-of select="'relation'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$elemName"/>
            </xsl:otherwise>
        </xsl:choose>      
    </xsl:function>
    
    
    <xsl:function name="func:mapRelTypeAttr" as="attribute()*">
        <xsl:param name="relType" as="xs:string"/>
        <xsl:variable name="primaryType" as="attribute()?">
            <xsl:if test="not(index-of(('other', 'unsure-rel', 'undersp-rel'), $relType) &gt; 0)">
                <xsl:attribute name="primaryType" select="(
                'cospec'[index-of(('ident'), $relType) &gt; 0],            
                'associative'[index-of(('element', 'element-inv', 'subset', 'subset-inv', 'poss', 'poss-inv'), $relType) &gt; 0]
                )[1]"/>
            </xsl:if>
        </xsl:variable>
        <xsl:if test="$primaryType">
            <xsl:copy-of select="$primaryType"/>
            <xsl:if test="not(index-of(('ident', 'element-inv', 'subset', 'subset-inv', 'poss-inv', 'other', 'unsure-rel', 'undersp-rel'), $relType) &gt; 0)">
                <xsl:attribute name="secondaryType" select="(
                    'setMember'[index-of(('element'), $relType) &gt; 0],            
                    'poss'[index-of(('poss'), $relType) &gt; 0]
                    )[1]"/>
            </xsl:if>
        </xsl:if>
    </xsl:function>
    
    
    <xsl:function name="func:mapAtt2ARRAU" as="attribute()?">
        <xsl:param name="attr" as="attribute()*"/>
        <xsl:for-each select="$attr">
            <xsl:variable name="currentAttr" select="." as="attribute()"/>
            <xsl:variable name="mappedAttName" select="(
                'type'[$currentAttr/name()=('AAcat', 'cat')],
                'gender'[$currentAttr/name()=('AAgen', 'gen')],
                'number'[$currentAttr/name()=('AAnum', 'num')],
                'person'[$currentAttr/name()=('AAper', 'per')],
                'headPos'[$currentAttr/name()='Lpos']
                )[1]" as="xs:string"/>
            <xsl:variable name="mappedAttValue" as="xs:string?" select="(
                concat('undersp-', (if($mappedAttName='person') then 'per' else (if($mappedAttName='number') then 'num' else $mappedAttName)))[$currentAttr='any'],
                'unmarked'[$currentAttr=''],
                $currentAttr[index-of(('fem', 'masc', 'neut', 'sing', 'plur', 'per1', 'per2', 'per3'), $currentAttr) &gt; 0],
                'namedEntity'[index-of(('the-pn', 'pn'), $currentAttr) &gt; 0],
                'nom'[index-of(('another-np', 'num-np', 'q-np', 'a-np', 'this-np', 'the-np', 'coord-np', 'poss-np', 'poss-pro', 'bare-np', 'that-pro', 'pers-pro', 'DT', 'NN', 'NNP', 'NNS'), $currentAttr) &gt; 0],
                'pron'[index-of(('PRP', 'PRP$'), $currentAttr) &gt; 0]
                )[1]"/>
            <xsl:if test="$mappedAttValue!=''">
                <xsl:attribute name="{$mappedAttName}" select="$mappedAttValue"/>
            </xsl:if>
        </xsl:for-each>
    </xsl:function>
        
    
    <!--Function returns a string which is used as a starting message in the template $templateName-->
    <xsl:function name="func:message4TemplateStart" as="xs:string">
        <xsl:param name="templateName" as="xs:string"/>
        <xsl:value-of select="concat(' # Template ', $templateName, ' started')"/>
    </xsl:function>
    
    
    <!-- Function determines first distinct char within two different strings -->
    <xsl:function name="func:first-distinct-char" as="xs:string+">
        <xsl:param name="string1" as="xs:string"/>
        <xsl:param name="string2" as="xs:string"/>
        <xsl:param name="position" as="xs:integer"/>
        <xsl:choose>
            <xsl:when test="($string1 != $string2) and ((string-length($string1) + string-length($string2)) &gt; 0)">
                <xsl:choose>
                    <xsl:when test="substring($string1, 1, 100) != substring($string2, 1, 100)">
                        <xsl:variable name="string1" select="substring($string1, 1, 100)" as="xs:string"/>
                        <xsl:variable name="string2" select="substring($string2, 1, 100)" as="xs:string"/>
                        <xsl:choose>
                            <xsl:when test="string-length($string1) = 0">
                                <xsl:value-of select="'primary-data (', string-length($primaryData_noWS), ') is shorter than text-data (', string-length($textData_noWS), ')'"/>
                            </xsl:when>
                            <xsl:when test="string-length($string2) = 0">
                                <xsl:value-of select="'text-data (', string-length($textData_noWS), ') is shorter than primary-data (', string-length($primaryData_noWS), ')'"/>
                            </xsl:when>
                            <xsl:when test="substring($string1, 1, 1) != substring($string2, 1, 1)">
                                <xsl:value-of select="concat('Position ', $position, ': ', substring($string1, 1, 1), '!=', substring($string2, 1, 1))"/>
                                <xsl:value-of select="concat('XML: ', substring($textData_noWS, $position - 20, 21))"/>
                                <xsl:value-of select="concat('TXT: ', substring($primaryData_noWS, $position - 20, 21))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="func:first-distinct-char(substring($string1, 2), substring($string2, 2), $position+1)"/>
                            </xsl:otherwise>
                        </xsl:choose>           
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="func:first-distinct-char(substring($string1, 100), substring($string2, 100), $position+100)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="'func:first-distinct-char() untersucht Texte mit identischem Zeicheninhalt'"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
    <xsl:function name="func:param-messages">
        <xsl:message select="'--------------------'"/>
        <xsl:message select="' Provided stylesheet parameters:'"/>
        <xsl:message select="concat('   primary-data=', $primary-data)"/>
        <xsl:message select="concat('   virtual-root=', ('/'[$virtual-root=''], $virtual-root)[1])"/>
        <xsl:message select="concat('   meta-root=', ('/'[$meta-root=''], $meta-root)[1])"/>
        <xsl:message select="concat('   local-xsd=', $local-xsd)"/>
        <xsl:message select="concat('   info-layer=', $info-layer)"/>
        <xsl:message select="concat('   copy-primary-data-to-sgf=', $copy-primary-data-to-sgf)"/>
        <xsl:message select="concat('   include-optional-elements=', $include-optional-elements)"/>
        <xsl:message select="concat('   include-ws-segments=', $include-ws-segments)"/>
        <xsl:message select="'--------------------'"/>
    </xsl:function>
    
    <xsl:function name="func:check-primary-data">
        <xsl:if test="empty($primary-data)">
            <xsl:message select="' *** ATTENTION: No stylesheet parameter primary-data declared!'"/>
            <xsl:message select="' *** ATTENTION: Generating primary data heuristically from source tree!'"/>
            <xsl:message select="'--------------------'"/>
        </xsl:if>
        <xsl:if test="$textData_noWS != $primaryData_noWS">
            <xsl:message select="' *** FATAL ERROR: Primary data and textual data mismatching!'"/>
            <xsl:message terminate="yes" select="string-join((' *** ', func:first-distinct-char($primaryData_noWS, $textData_noWS, 1)), ' ')"/>
        </xsl:if>
    </xsl:function>
    
    <xsl:function name="func:return-ID" as="xs:string">
        <xsl:param name="input-string" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="matches($input-string,  '^[_a-zA-Z][-a-zA-Z0-9\.äöüÄÖÜß_]*$')">
                <!--no problem: $input-String is valid with respect to type xs:ID-->
                <xsl:copy-of select="$input-string"/>
            </xsl:when>
            <xsl:otherwise>
                <!--$input-String is not valid with respect to type xs:ID-->
                <xsl:choose>
                    <xsl:when test="matches(substring($input-string, 1, 1), '[_A-Za-z]')">
                        <!--starting char of $input-String is valid with respect to type xs:ID-->
                        <xsl:copy-of select="func:return-ID(replace($input-string, '[^-a-zA-Z0-9\.äöüÄÖÜß_]', '_'))"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <!--starting char of $input-String is not valid with respect to type xs:ID-->
                        <xsl:copy-of select="func:return-ID(concat('_', $input-string))"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
    <!-- ############################################ -->
        
</xsl:stylesheet>
