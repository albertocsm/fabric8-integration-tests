/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.pires.example.dal.entities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.json.JSONObject;

import java.io.IOException;

public class JSONObjectSerializer extends JsonSerializer<JSONObject>
{
    @Override
    public void serialize(JSONObject value, JsonGenerator jsonGenerator, SerializerProvider provider)
            throws IOException
    {
        jsonGenerator.writeStartObject();
        constructObject(value, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private void constructObject(JSONObject object, JsonGenerator jsonGenerator) throws IOException
    {
        for (String key : JSONObject.getNames(object))
        {
            Object value = object.get(key);

            if (value instanceof JSONObject)
            {
                jsonGenerator.writeFieldName(key);
                jsonGenerator.writeStartObject();
                constructObject(object.getJSONObject(key), jsonGenerator);
                jsonGenerator.writeEndObject();
            }
            else
            {
                jsonGenerator.writeStringField(key, value.toString());
            }
        }
    }


}